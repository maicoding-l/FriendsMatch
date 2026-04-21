package com.mai.friendsFinder.model.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 评论VO（返回给前端）
 */
@Data
public class PostCommentVO {
    /**
     * 评论ID
     */
    private Long id;

    /**
     * 所属帖子ID
     */
    private Long postId;

    /**
     * 评论用户ID
     */
    private Long userId;

    /**
     * 评论用户信息
     */
    private UserVO user;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 父评论ID
     */
    private Long parentId;

    /**
     * 回复给哪个用户
     */
    private Long replyToUserId;

    /**
     * 回复给哪个用户的信息
     */
    private UserVO replyToUser;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 子评论列表（回复）
     */
    private List<PostCommentVO> replies;
}
