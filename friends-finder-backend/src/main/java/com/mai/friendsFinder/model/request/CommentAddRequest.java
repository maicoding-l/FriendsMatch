package com.mai.friendsFinder.model.request;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 评论/回复请求
 */
@Data
public class CommentAddRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 帖子ID
     */
    private Long postId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 父评论ID（回复评论时使用）
     */
    private Long parentId;

    /**
     * 回复给哪个用户
     */
    private Long replyToUserId;
}
