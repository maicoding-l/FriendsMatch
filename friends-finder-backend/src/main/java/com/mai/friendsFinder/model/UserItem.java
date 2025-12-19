package com.mai.friendsFinder.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 用户-物品关系
 * @TableName user_item
 */
@TableName(value ="user_item")
@Data
public class UserItem {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 
     */
    private Long userId;

    /**
     * 
     */
    private Long itemId;

    /**
     * 1-喜欢 2-收藏 3-看过/读过/听过
     */
    private Integer action;

    /**
     * 边权重
     */
    private Double weight;

    /**
     *  创建时间
     */
    private Date createTime;

    /**
     *  更新时间
     */
    private Date updateTime;

    /**
     * 逻辑删除标志
     */
    @TableLogic
    private Integer isDelete;
}