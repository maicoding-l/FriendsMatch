package com.mai.friendsFinder.model.request;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 发帖请求
 */
@Data
public class PostAddRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 小组ID
     */
    private Long teamId;

    /**
     * 帖子标题
     */
    private String title;

    /**
     * 帖子内容
     */
    private String content;
}
