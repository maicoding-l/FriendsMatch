package com.mai.friendsFinder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mai.friendsFinder.common.ErrorCode;
import com.mai.friendsFinder.exception.BusinessException;
import com.mai.friendsFinder.model.Item;
import com.mai.friendsFinder.model.UserItem;
import com.mai.friendsFinder.model.vo.UserItemVO;
import com.mai.friendsFinder.service.UserItemService;
import com.mai.friendsFinder.mapper.ItemMapper;
import com.mai.friendsFinder.mapper.UserItemMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
* @author ljm
* @description 针对表【user_item(用户-物品关系)】的数据库操作Service实现
* @createDate 2025-12-17 10:12:33
*/
@Slf4j
@Service
public class UserItemServiceImpl extends ServiceImpl<UserItemMapper, UserItem>
    implements UserItemService{

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private ItemMapper itemMapper;

    /**
     * Redis 缓存 key 前缀
     */
    private static final String ITEM_RECOMMEND_KEY_PREFIX = "mai:recommend:randomwalk:";
    private static final String USER_MATCH_KEY_PREFIX = "mai:match:users:";

    /**
     * 切换收藏状态
     * @param userId 用户ID
     * @param itemId 物品ID
     * @return 是否收藏（true-已收藏，false-已取消收藏）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean toggleFavorite(Long userId, Long itemId) {
        if (userId == null || userId <= 0 || itemId == null || itemId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID或物品ID无效");
        }

        // 查询是否已有记录
        QueryWrapper<UserItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId).eq("item_id", itemId);
        UserItem existingRecord = this.getOne(queryWrapper);

        if (existingRecord == null) {
            // 无记录：插入新数据，action=2(收藏)，weight=5.0
            UserItem newUserItem = new UserItem();
            newUserItem.setUserId(userId);
            newUserItem.setItemId(itemId);
            newUserItem.setAction(2);  // 收藏
            newUserItem.setWeight(5.0);

            boolean result = this.save(newUserItem);
            if (!result) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "收藏失败");
            }
            log.info("用户 {} 收藏了物品 {}", userId, itemId);
            // 清除推荐缓存
            clearUserRecommendCache(userId);
            return true;  // 已收藏
        } else {
            // 有记录：判断当前action
            Integer currentAction = existingRecord.getAction();

            if (currentAction == 2) {
                // 当前已是收藏状态，取消收藏（删除记录）
                boolean result = this.removeById(existingRecord.getId());
                if (!result) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "取消收藏失败");
                }
                log.info("用户 {} 取消收藏物品 {}", userId, itemId);
                // 清除推荐缓存
                clearUserRecommendCache(userId);
                return false;  // 已取消收藏
            } else {
                // 当前不是收藏状态，更新为收藏，weight保持不变
                existingRecord.setAction(2);
                boolean result = this.updateById(existingRecord);
                if (!result) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "收藏失败");
                }
                log.info("用户 {} 收藏了物品 {}（从action={}更新）", userId, itemId, currentAction);
                // 清除推荐缓存
                clearUserRecommendCache(userId);
                return true;  // 已收藏
            }
        }
    }

    /**
     * 打分
     * @param userId 用户ID
     * @param itemId 物品ID
     * @param score 分数 (1.0-5.0)
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean rateItem(Long userId, Long itemId, Double score) {
        if (userId == null || userId <= 0 || itemId == null || itemId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID或物品ID无效");
        }
        if (score == null || score < 1.0 || score > 5.0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分数必须在1.0到5.0之间");
        }

        // 根据分数推导action：score>=4.0则action=1(喜欢)，否则action=3(看过)
        int newAction = (score >= 4.0) ? 1 : 3;

        // 查询是否已有记录
        QueryWrapper<UserItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId).eq("item_id", itemId);
        UserItem existingRecord = this.getOne(queryWrapper);

        if (existingRecord == null) {
            // 无记录：插入新数据
            UserItem newUserItem = new UserItem();
            newUserItem.setUserId(userId);
            newUserItem.setItemId(itemId);
            newUserItem.setAction(newAction);
            newUserItem.setWeight(score);

            boolean result = this.save(newUserItem);
            if (!result) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "打分失败");
            }
            log.info("用户 {} 对物品 {} 打分 {}，action={}", userId, itemId, score, newAction);
            // 清除推荐缓存
            clearUserRecommendCache(userId);
            return true;
        } else {
            // 有记录：更新weight，如果原action!=2(非收藏)，则更新action
            existingRecord.setWeight(score);

            Integer currentAction = existingRecord.getAction();
            if (currentAction != 2) {
                // 如果原来不是收藏状态，则更新action
                existingRecord.setAction(newAction);
                log.info("用户 {} 对物品 {} 打分 {}，action从{}更新为{}", userId, itemId, score, currentAction, newAction);
            } else {
                // 如果原来是收藏状态，保持action=2不变，只更新weight
                log.info("用户 {} 对物品 {} 打分 {}，保持收藏状态(action=2)，只更新权重", userId, itemId, score);
            }

            boolean result = this.updateById(existingRecord);
            if (!result) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "打分失败");
            }
            // 清除推荐缓存
            clearUserRecommendCache(userId);
            return true;
        }
    }

    /**
     * 获取用户对物品的交互状态
     * @param userId 用户ID
     * @param itemId 物品ID
     * @return UserItem对象，不存在返回null
     */
    @Override
    public UserItem getUserItemAction(Long userId, Long itemId) {
        if (userId == null || userId <= 0 || itemId == null || itemId <= 0) {
            return null;
        }

        QueryWrapper<UserItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId).eq("item_id", itemId);
        return this.getOne(queryWrapper);
    }

    /**
     * 获取用户已标记的物品列表
     * @param userId 用户ID
     * @param itemType 物品类型（1-书籍 2-电影 3-音乐），为null时返回所有类型
     * @return 用户已标记的物品列表
     */
    @Override
    public List<UserItemVO> getUserMarkedItems(Long userId, Integer itemType) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID无效");
        }

        // 查询用户所有标记记录
        QueryWrapper<UserItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.orderByDesc("create_time");
        List<UserItem> userItemList = this.list(queryWrapper);

        List<UserItemVO> result = new ArrayList<>();
        for (UserItem userItem : userItemList) {
            // 直接使用 ItemMapper 查询，避免通过 ItemService 造成循环依赖
            Item item = itemMapper.selectById(userItem.getItemId());
            if (item == null) {
                continue;
            }

            // 如果指定了物品类型，进行过滤
            if (itemType != null && !itemType.equals(item.getItemType())) {
                continue;
            }

            // 构建VO
            UserItemVO vo = new UserItemVO();
            vo.setItem(item);
            vo.setAction(userItem.getAction());
            vo.setWeight(userItem.getWeight());
            vo.setCreateTime(userItem.getCreateTime());
            result.add(vo);
        }

        return result;
    }

    /**
     * 清除用户的推荐缓存
     * 包括物品推荐和用户匹配两个缓存
     *
     * @param userId 用户ID
     */
    private void clearUserRecommendCache(Long userId) {
        if (userId == null || userId <= 0) {
            return;
        }

        try {
            // 清除物品推荐缓存
            String itemRecommendKey = ITEM_RECOMMEND_KEY_PREFIX + userId;
            Boolean itemDeleted = redisTemplate.delete(itemRecommendKey);

            // 清除用户匹配缓存
            String userMatchKey = USER_MATCH_KEY_PREFIX + userId;
            Boolean userDeleted = redisTemplate.delete(userMatchKey);

            log.info("已清除用户 {} 的推荐缓存，物品推荐: {}, 用户匹配: {}",
                     userId, itemDeleted, userDeleted);
        } catch (Exception e) {
            log.error("清除用户 {} 的推荐缓存失败", userId, e);
            // 清除缓存失败不影响主流程，仅记录日志
        }
    }
}




