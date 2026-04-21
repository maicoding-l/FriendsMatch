package com.mai.friendsFinder.service;

import com.mai.friendsFinder.model.UserItem;
import com.mai.friendsFinder.model.vo.UserItemVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author ljm
* @description 针对表【user_item(用户-物品关系)】的数据库操作Service
* @createDate 2025-12-17 10:12:33
*/
public interface UserItemService extends IService<UserItem> {

    /**
     * 切换收藏状态
     * @param userId 用户ID
     * @param itemId 物品ID
     * @return 是否收藏（true-已收藏，false-已取消收藏）
     */
    Boolean toggleFavorite(Long userId, Long itemId);

    /**
     * 打分
     * @param userId 用户ID
     * @param itemId 物品ID
     * @param score 分数 (1.0-5.0)
     * @return 是否成功
     */
    Boolean rateItem(Long userId, Long itemId, Double score);

    /**
     * 获取用户对物品的交互状态
     * @param userId 用户ID
     * @param itemId 物品ID
     * @return UserItem对象，不存在返回null
     */
    UserItem getUserItemAction(Long userId, Long itemId);

    /**
     * 获取用户已标记的物品列表
     * @param userId 用户ID
     * @param itemType 物品类型（1-书籍 2-电影 3-音乐），为null时返回所有类型
     * @return 用户已标记的物品列表
     */
    List<UserItemVO> getUserMarkedItems(Long userId, Integer itemType);
}
