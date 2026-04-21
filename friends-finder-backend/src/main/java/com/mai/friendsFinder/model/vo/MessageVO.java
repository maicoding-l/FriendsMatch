package com.mai.friendsFinder.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 消息视图对象
 */
@Data
public class MessageVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 消息ID
     */
    private Long id;

    /**
     * 发送者用户ID
     */
    private Long fromUserId;

    /**
     * 发送者用户名
     */
    private String fromUsername;

    /**
     * 发送者头像
     */
    private String fromUserAvatar;

    /**
     * 接收者用户ID
     */
    private Long toUserId;

    /**
     * 接收者用户名
     */
    private String toUsername;

    /**
     * 接收者头像
     */
    private String toUserAvatar;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息类型：0-文本，1-图片，2-文件
     */
    private Integer type;

    /**
     * 消息状态：0-未读，1-已读
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;
}
