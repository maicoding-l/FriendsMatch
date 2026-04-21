package com.mai.friendsFinder.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 处理好友申请请求
 */
@Data
public class FriendRequestHandleRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 申请id
     */
    private Long requestId;

    /**
     * 操作类型: 1-同意, 2-拒绝
     */
    private Integer action;
}
