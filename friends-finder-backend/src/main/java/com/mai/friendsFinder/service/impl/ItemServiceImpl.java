package com.mai.friendsFinder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mai.friendsFinder.common.ErrorCode;
import com.mai.friendsFinder.exception.BusinessException;
import com.mai.friendsFinder.model.Item;
import com.mai.friendsFinder.model.UserItem;
import com.mai.friendsFinder.model.request.ItemAddRequest;
import com.mai.friendsFinder.model.request.ItemUpdateRequest;
import com.mai.friendsFinder.service.ItemService;
import com.mai.friendsFinder.mapper.ItemMapper;
import com.mai.friendsFinder.service.UserItemService;
import com.mai.friendsFinder.service.UserService;
import com.mai.friendsFinder.utils.RecommendUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author ljm
* @description 针对表【item(物品表)】的数据库操作Service实现
* @createDate 2025-12-17 10:12:10
*/
@Slf4j
@Service
public class ItemServiceImpl extends ServiceImpl<ItemMapper, Item>
    implements ItemService{

    @Resource
    private UserService userService;

    @Resource
    private UserItemService userItemService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public Long addItem(ItemAddRequest itemAddRequest, HttpServletRequest request) {
        // 鉴权：只有管理员可以新增物品
        if (!userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        if (itemAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 校验必填字段
        String title = itemAddRequest.getTitle();
        Integer itemType = itemAddRequest.getItemType();
        if (StringUtils.isBlank(title) || itemType == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标题和类型不能为空");
        }
        // 校验物品类型：1-书籍 2-电影 3-音乐
        if (itemType < 1 || itemType > 3) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "物品类型必须为 1-书籍、2-电影、3-音乐");
        }
        // 创建物品对象
        Item item = new Item();
        BeanUtils.copyProperties(itemAddRequest, item);
        item.setPopularity(0); // 初始热度为0
        boolean result = this.save(item);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "新增物品失败");
        }
        return item.getId();
    }

    @Override
    public Boolean deleteItem(Long id, HttpServletRequest request) {
        // 鉴权：只有管理员可以删除物品
        if (!userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "物品ID无效");
        }
        return this.removeById(id);
    }

    @Override
    public Boolean updateItem(ItemUpdateRequest itemUpdateRequest, HttpServletRequest request) {
        // 鉴权：只有管理员可以修改物品
        if (!userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        if (itemUpdateRequest == null || itemUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "更新请求或物品ID不能为空");
        }
        Long id = itemUpdateRequest.getId();
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "物品ID无效");
        }
        // 检查物品是否存在
        Item existItem = this.getById(id);
        if (existItem == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "物品不存在");
        }
        // 校验物品类型（如果提供）
        Integer itemType = itemUpdateRequest.getItemType();
        if (itemType != null && (itemType < 1 || itemType > 3)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "物品类型必须为 1-书籍、2-电影、3-音乐");
        }
        // 更新物品信息
        Item item = new Item();
        BeanUtils.copyProperties(itemUpdateRequest, item);
        return this.updateById(item);
    }

    @Override
    public List<Item> searchItems(String title, String creator, Integer itemType, String tags) {
        QueryWrapper<Item> queryWrapper = new QueryWrapper<>();
        // 按标题模糊搜索
        if (StringUtils.isNotBlank(title)) {
            queryWrapper.like("title", title);
        }
        // 按作者/导演/艺术家模糊搜索
        if (StringUtils.isNotBlank(creator)) {
            queryWrapper.like("creator", creator);
        }
        // 按类型精确搜索
        if (itemType != null) {
            if (itemType < 1 || itemType > 3) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "物品类型必须为 1-书籍、2-电影、3-音乐");
            }
            queryWrapper.eq("item_type", itemType);
        }
        // 按标签模糊搜索
        if (StringUtils.isNotBlank(tags)) {
            queryWrapper.like("tags", tags);
        }
        // 按热度和创建时间降序排列
        queryWrapper.orderByDesc("popularity", "create_time");
        return this.list(queryWrapper);
    }

    @Override
    public List<Item> recommendItems(Integer limit) {
        if (limit == null || limit <= 0 || limit > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "推荐数量范围需在 1-50 之间");
        }
        QueryWrapper<Item> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("popularity", "create_time")
                .last("limit " + limit);
        return this.list(queryWrapper);
    }

    @Override
    public Item getItemById(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "物品ID无效");
        }
        Item item = this.getById(id);
        if (item == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "物品不存在");
        }
        return item;
    }

    @Override
    public List<Item> recommendItemsByRandomWalk(Long userId, Integer topN) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID无效");
        }
        if (topN == null || topN <= 0) {
            topN = 20; // 默认返回20个推荐
        }
        if (topN > 100) {
            topN = 100; // 最多返回100个推荐
        }

        // Redis 缓存 key
        String redisKey = "mai:recommend:randomwalk:" + userId;

        try {
            // 1. 优先从 Redis 缓存获取推荐结果
            Object cachedResult = redisTemplate.opsForValue().get(redisKey);
            if (cachedResult != null && cachedResult instanceof List) {
                @SuppressWarnings("unchecked")
                List<Item> cachedItems = (List<Item>) cachedResult;
                log.info("从缓存获取用户 {} 的推荐结果，共 {} 个物品", userId, cachedItems.size());

                // 1.1 获取用户已标记的物品ID列表（缓存期间可能新增标记）
                QueryWrapper<UserItem> userItemQueryWrapper = new QueryWrapper<>();
                userItemQueryWrapper.eq("user_id", userId);
                userItemQueryWrapper.select("item_id");
                List<Long> markedItemIds = userItemService.list(userItemQueryWrapper).stream()
                        .map(UserItem::getItemId)
                        .collect(Collectors.toList());

                // 1.2 从缓存结果中过滤掉已标记的物品
                List<Item> filteredItems = cachedItems.stream()
                        .filter(item -> !markedItemIds.contains(item.getId()))
                        .collect(Collectors.toList());

                log.info("缓存命中后过滤，用户 {} 过滤前 {} 个，过滤后 {} 个物品",
                         userId, cachedItems.size(), filteredItems.size());

                // 如果过滤后的数量大于等于请求的topN，截取返回
                if (filteredItems.size() >= topN) {
                    return filteredItems.subList(0, topN);
                }
                // 否则返回全部过滤后的结果
                return filteredItems;
            }

            log.info("缓存未命中，开始实时计算用户 {} 的推荐结果", userId);

            // 2. 缓存未命中，实时计算推荐结果
            // 2.1 从数据库加载所有用户-物品关系数据
            List<UserItem> allUserItems = userItemService.list();
            if (allUserItems == null || allUserItems.isEmpty()) {
                log.warn("用户-物品关系数据为空，无法生成推荐");
                return Collections.emptyList();
            }

            // 2.2 创建推荐工具实例，构建图
            RecommendUtils recommendUtils = new RecommendUtils(allUserItems);
            log.info("推荐图构建完成，节点数: {}, 边数: {}",
                     recommendUtils.getNodeCount(),
                     recommendUtils.getEdgeCount());

            // 2.3 执行随机游走算法，获取推荐的物品ID列表（计算更多结果用于缓存）
            List<Long> recommendedItemIds = recommendUtils.getRecommendation(userId, 100);
            if (recommendedItemIds == null || recommendedItemIds.isEmpty()) {
                log.info("为用户 {} 未生成推荐结果", userId);
                return Collections.emptyList();
            }

            // 2.3.1 获取用户已标记的物品ID列表
            QueryWrapper<UserItem> userItemQueryWrapper = new QueryWrapper<>();
            userItemQueryWrapper.eq("user_id", userId);
            userItemQueryWrapper.select("item_id");
            List<Long> markedItemIds = userItemService.list(userItemQueryWrapper).stream()
                    .map(UserItem::getItemId)
                    .collect(Collectors.toList());
            log.info("用户 {} 已标记 {} 个物品，将从推荐中排除", userId, markedItemIds.size());

            // 2.4 根据物品ID列表查询物品详情，并过滤掉已标记的物品
            List<Item> recommendedItems = recommendedItemIds.stream()
                    .filter(itemId -> !markedItemIds.contains(itemId)) // 过滤掉已标记的物品
                    .map(this::getById)
                    .filter(item -> item != null) // 过滤掉不存在的物品
                    .collect(Collectors.toList());

            // 2.5 将计算结果缓存到 Redis（24小时过期）
            if (!recommendedItems.isEmpty()) {
                redisTemplate.opsForValue().set(
                    redisKey,
                    recommendedItems,
                    24 * 60 * 60 * 1000,
                    java.util.concurrent.TimeUnit.MILLISECONDS
                );
                log.info("已将用户 {} 的推荐结果缓存到 Redis，共 {} 个物品", userId, recommendedItems.size());
            }

            // 2.6 返回请求数量的结果
            log.info("为用户 {} 成功生成 {} 个推荐物品", userId, recommendedItems.size());
            if (recommendedItems.size() > topN) {
                return recommendedItems.subList(0, topN);
            }
            return recommendedItems;

        } catch (BusinessException e) {
            // 业务异常直接抛出
            throw e;
        } catch (Exception e) {
            log.error("为用户 {} 生成推荐时发生异常", userId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成推荐失败：" + e.getMessage());
        }
    }
}




