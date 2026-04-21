package com.mai.friendsFinder.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mai.friendsFinder.mapper.FriendMapper;
import com.mai.friendsFinder.mapper.ItemMapper;
import com.mai.friendsFinder.model.Friend;
import com.mai.friendsFinder.model.Item;
import com.mai.friendsFinder.model.User;
import com.mai.friendsFinder.model.UserItem;
import com.mai.friendsFinder.model.vo.UserMatchVO;
import com.mai.friendsFinder.service.ItemService;
import com.mai.friendsFinder.service.UserItemService;
import com.mai.friendsFinder.service.UserService;
import com.mai.friendsFinder.utils.RecommendUtils;
import com.mai.friendsFinder.utils.UserRecommendUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 随机游走推荐预缓存定时任务
 *
 * @author ljm
 * @description 定时预计算用户的物品推荐结果并存入Redis，减少实时计算压力
 */
@Component
@Slf4j
public class RecommendCacheJob {

    @Resource
    private UserService userService;

    @Resource
    private ItemService itemService;

    @Resource
    private UserItemService userItemService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private ItemMapper itemMapper;

    @Resource
    private FriendMapper friendMapper;

    /**
     * Redis key 前缀 - 物品推荐
     */
    private static final String REDIS_KEY_PREFIX_ITEM = "mai:recommend:randomwalk:";

    /**
     * Redis key 前缀 - 用户匹配
     */
    private static final String REDIS_KEY_PREFIX_USER = "mai:match:users:";

    /**
     * 缓存过期时间：24小时
     */
    private static final long CACHE_EXPIRE_TIME = 24 * 60 * 60 * 1000;

    /**
     * 推荐结果数量
     */
    private static final int RECOMMEND_TOP_N = 100;

    /**
     * 定时任务：每天凌晨2点执行
     * 预缓存所有有交互记录的用户的推荐结果（物品推荐 + 用户匹配）
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void preCacheRecommendations() {
        // 使用分布式锁，避免多实例重复执行
        RLock lock = redissonClient.getLock("mai:preCacheJob:recommend:lock");
        try {
            // 尝试获取锁，最多等待0秒，锁最长持有30分钟
            if (lock.tryLock(0, 30, TimeUnit.MINUTES)) {
                log.info("开始执行推荐预缓存定时任务（物品推荐 + 用户匹配）");
                long startTime = System.currentTimeMillis();

                // 1. 获取所有用户-物品关系数据
                List<UserItem> allUserItems = userItemService.list();
                if (allUserItems == null || allUserItems.isEmpty()) {
                    log.warn("用户-物品关系数据为空，跳过预缓存任务");
                    return;
                }

                // 2. 构建推荐图（复用同一个图实例，避免重复构建）
                RecommendUtils itemRecommendUtils = new RecommendUtils(allUserItems);
                UserRecommendUtils userRecommendUtils = new UserRecommendUtils(allUserItems);
                log.info("推荐图构建完成 - 物品推荐图：节点数={}, 边数={}",
                        itemRecommendUtils.getNodeCount(),
                        itemRecommendUtils.getEdgeCount());
                log.info("推荐图构建完成 - 用户推荐图：节点数={}, 边数={}",
                        userRecommendUtils.getNodeCount(),
                        userRecommendUtils.getEdgeCount());

                // 3. 获取所有有交互记录的用户ID（去重）
                List<Long> userIds = allUserItems.stream()
                        .map(UserItem::getUserId)
                        .distinct()
                        .collect(Collectors.toList());

                log.info("开始为 {} 个用户预缓存推荐结果", userIds.size());

                // 4. 为每个用户计算并缓存推荐结果
                int itemSuccessCount = 0;
                int itemFailCount = 0;
                int userSuccessCount = 0;
                int userFailCount = 0;

                for (Long userId : userIds) {
                    // 4.1 缓存物品推荐结果（并过滤已标记的物品）
                    try {
                        List<Long> recommendedItemIds = itemRecommendUtils.getRecommendation(userId, RECOMMEND_TOP_N);

                        if (recommendedItemIds != null && !recommendedItemIds.isEmpty()) {
                            // 4.1.1 查询用户已标记的物品ID列表
                            QueryWrapper<UserItem> userItemQueryWrapper = new QueryWrapper<>();
                            userItemQueryWrapper.eq("user_id", userId);
                            userItemQueryWrapper.select("item_id");
                            List<Long> markedItemIds = userItemService.list(userItemQueryWrapper).stream()
                                    .map(UserItem::getItemId)
                                    .collect(Collectors.toList());

                            // 4.1.2 过滤掉已标记的物品
                            List<Item> recommendedItems = recommendedItemIds.stream()
                                    .filter(itemId -> !markedItemIds.contains(itemId)) // 过滤已标记
                                    .map(itemService::getById)
                                    .filter(item -> item != null)
                                    .collect(Collectors.toList());

                            // 只有在有推荐结果时才缓存
                            if (!recommendedItems.isEmpty()) {
                                String redisKey = REDIS_KEY_PREFIX_ITEM + userId;
                                redisTemplate.opsForValue().set(redisKey, recommendedItems, CACHE_EXPIRE_TIME, TimeUnit.MILLISECONDS);

                                itemSuccessCount++;
                                log.debug("用户 {} 的物品推荐结果已缓存（已过滤已标记），共 {} 个物品", userId, recommendedItems.size());
                            }
                        }
                    } catch (Exception e) {
                        itemFailCount++;
                        log.error("为用户 {} 缓存物品推荐结果失败", userId, e);
                    }

                    // 4.2 缓存用户匹配结果（并过滤已有好友）
                    try {
                        List<Long> recommendedUserIds = userRecommendUtils.getSimilarUsers(userId, 100);

                        if (recommendedUserIds != null && !recommendedUserIds.isEmpty()) {
                            // 4.2.1 查询用户的所有好友ID列表（双向查询）
                            QueryWrapper<Friend> friendQueryWrapper = new QueryWrapper<>();
                            friendQueryWrapper.eq("user_id", userId).or().eq("friend_id", userId);
                            friendQueryWrapper.select("user_id", "friend_id");
                            List<Friend> friendList = friendMapper.selectList(friendQueryWrapper);

                            Set<Long> friendIds = new HashSet<>();
                            for (Friend friend : friendList) {
                                if (!friend.getUserId().equals(userId)) {
                                    friendIds.add(friend.getUserId());
                                }
                                if (!friend.getFriendId().equals(userId)) {
                                    friendIds.add(friend.getFriendId());
                                }
                            }

                            // 4.2.2 过滤掉已是好友的用户
                            List<Long> filteredUserIds = recommendedUserIds.stream()
                                    .filter(matchedUserId -> !friendIds.contains(matchedUserId))
                                    .collect(Collectors.toList());

                            if (filteredUserIds.isEmpty()) {
                                log.debug("用户 {} 的推荐用户全部为好友，无需缓存", userId);
                                continue;
                            }

                            // 4.2.3 查询用户详情
                            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
                            queryWrapper.in("id", filteredUserIds);
                            Map<Long, List<User>> userIdUserListMap = userService.list(queryWrapper).stream()
                                    .map(userService::getSafetyUser)
                                    .collect(Collectors.groupingBy(User::getId));

                            // 4.2.4 构建UserMatchVO列表
                            List<UserMatchVO> matchVOList = new ArrayList<>();
                            for (Long matchedUserId : filteredUserIds) {
                                List<User> users = userIdUserListMap.get(matchedUserId);
                                if (users == null || users.isEmpty()) {
                                    continue;
                                }

                                User matchedUser = users.get(0);
                                UserMatchVO matchVO = new UserMatchVO();
                                matchVO.setUser(matchedUser);

                                // 获取共同喜欢的物品ID
                                List<Long> commonItemIds = userRecommendUtils.getCommonItems(userId, matchedUserId, 5);
                                if (commonItemIds != null && !commonItemIds.isEmpty()) {
                                    List<Item> commonItems = commonItemIds.stream()
                                            .map(itemId -> itemMapper.selectById(itemId))
                                            .filter(item -> item != null)
                                            .collect(Collectors.toList());
                                    matchVO.setCommonItems(commonItems);
                                } else {
                                    matchVO.setCommonItems(new ArrayList<>());
                                }

                                matchVOList.add(matchVO);
                            }

                            // 4.2.5 存入Redis
                            if (!matchVOList.isEmpty()) {
                                String redisKey = REDIS_KEY_PREFIX_USER + userId;
                                redisTemplate.opsForValue().set(redisKey, matchVOList, CACHE_EXPIRE_TIME, TimeUnit.MILLISECONDS);

                                userSuccessCount++;
                                log.debug("用户 {} 的用户匹配结果已缓存（已过滤好友），共 {} 个用户", userId, matchVOList.size());
                            }
                        }
                    } catch (Exception e) {
                        userFailCount++;
                        log.error("为用户 {} 缓存用户匹配结果失败", userId, e);
                    }
                }

                long endTime = System.currentTimeMillis();
                log.info("推荐预缓存任务执行完成，耗时: {} ms", (endTime - startTime));
                log.info("物品推荐缓存 - 成功: {}, 失败: {}", itemSuccessCount, itemFailCount);
                log.info("用户匹配缓存 - 成功: {}, 失败: {}", userSuccessCount, userFailCount);

            } else {
                log.info("未能获取锁，跳过本次推荐预缓存任务");
            }
        } catch (InterruptedException e) {
            log.error("推荐预缓存任务执行异常", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("推荐预缓存任务执行异常", e);
        } finally {
            // 释放锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("推荐预缓存任务锁已释放");
            }
        }
    }

    /**
     * 手动触发预缓存任务（用于测试）
     * 可以通过管理接口调用此方法
     */
    public void triggerCacheManually() {
        log.info("手动触发推荐预缓存任务");
        preCacheRecommendations();
    }
}
