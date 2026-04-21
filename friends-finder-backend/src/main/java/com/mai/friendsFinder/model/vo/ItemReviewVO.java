package com.mai.friendsFinder.model.vo;

import com.mai.friendsFinder.model.User;
import lombok.Data;

import java.util.Date;

/**
 * 物品短评VO（包含用户信息和点赞状态）
 */
@Data
public class ItemReviewVO {
    /**
     * 短评ID
     */
    private Long id;

    /**
     * 用户信息（脱敏后）
     */
    private User user;

    /**
     * 物品ID
     */
    private Long itemId;

    /**
     * 短评内容
     */
    private String content;

    /**
     * 评分（1.0-5.0）
     */
    private Double rating;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 当前用户是否已点赞
     */
    private Boolean isLiked;

    /**
     * 创建时间
     */
    private Date createTime;
}
