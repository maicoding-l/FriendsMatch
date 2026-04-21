package com.mai.friendsFinder.model.vo;

import com.mai.friendsFinder.model.Item;
import lombok.Data;

import java.util.Date;

/**
 * 用户已标记物品VO
 */
@Data
public class UserItemVO {
    /**
     * 物品信息
     */
    private Item item;

    /**
     * 用户标记的动作 1-喜欢 2-收藏 3-看过/读过/听过
     */
    private Integer action;

    /**
     * 权重/评分
     */
    private Double weight;

    /**
     * 标记时间
     */
    private Date createTime;
}
