package com.mai.friendsFinder.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 物品表
 * @TableName item
 */
@TableName(value ="item")
@Data
public class Item {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
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

    /**
     * 热度（点赞/收藏等统计）
     */
    private Integer popularity;

    /**
     * 
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 逻辑删除标志
     */
    @TableLogic
    private Integer isDelete;
}