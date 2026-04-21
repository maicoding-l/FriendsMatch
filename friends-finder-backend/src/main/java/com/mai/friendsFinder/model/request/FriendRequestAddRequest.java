package com.mai.friendsFinder.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 发送好友申请请求
 */
@Data
public class FriendRequestAddRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 接收申请的用户id
     */
    private Long toUserId;

    /**
     * 申请消息
     */
    private String message;
}
