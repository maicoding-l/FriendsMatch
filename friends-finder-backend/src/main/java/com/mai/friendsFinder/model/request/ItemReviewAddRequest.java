package com.mai.friendsFinder.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 添加物品短评请求
 */
@Data
public class ItemReviewAddRequest implements Serializable {
    private static final long serialVersionUID = 1L;

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
}
