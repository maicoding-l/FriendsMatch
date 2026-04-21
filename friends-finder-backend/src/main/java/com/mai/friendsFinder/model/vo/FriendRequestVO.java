package com.mai.friendsFinder.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 好友申请视图对象
 */
@Data
public class FriendRequestVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 申请id
     */
    private Long id;

    /**
     * 发起申请的用户信息
     */
    private UserVO fromUser;

    /**
     * 接收申请的用户信息
     */
    private UserVO toUser;

    /**
     * 状态：0-待处理，1-已同意，2-已拒绝
     */
    private Integer status;

    /**
     * 申请消息
     */
    private String message;

    /**
     * 创建时间
     */
    private Date createTime;
}
