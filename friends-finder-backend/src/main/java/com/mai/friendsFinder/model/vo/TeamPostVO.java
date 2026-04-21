package com.mai.friendsFinder.model.vo;

import lombok.Data;

import java.util.Date;

/**
 * 帖子VO（返回给前端）
 */
@Data
public class TeamPostVO {
    /**
     * 帖子ID
     */
    private Long id;

    /**
     * 所属小组ID
     */
    private Long teamId;

    /**
     * 发帖用户ID
     */
    private Long userId;

    /**
     * 发帖用户信息
     */
    private UserVO user;

    /**
     * 帖子标题
     */
    private String title;

    /**
     * 帖子内容
     */
    private String content;

    /**
     * 浏览次数
     */
    private Integer viewCount;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 评论数
     */
    private Integer commentCount;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
