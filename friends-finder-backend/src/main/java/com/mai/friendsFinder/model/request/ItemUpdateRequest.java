package com.mai.friendsFinder.model.request;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 物品更新请求
 */
@Data
public class ItemUpdateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 2456328195618739215L;

    /**
     * 物品ID
     */
    private Long id;

    /**
     * 类型：1-书籍 2-电影 3-音乐
     */
    private Integer itemType;

    /**
     * 名称
     */
    private String title;

    /**
     * 封面图片
     */
    private String coverUrl;

    /**
     * 简介/摘要
     */
    private String description;

    /**
     * 作者 / 导演 / 艺术家
     */
    private String creator;

    /**
     * 发行/出版年份
     */
    private Integer publishYear;

    /**
     * 标签（逗号分隔）
     */
    private String tags;
}
