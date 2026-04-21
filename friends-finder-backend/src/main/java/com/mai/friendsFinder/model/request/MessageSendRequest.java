package com.mai.friendsFinder.model.request;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 发送消息请求
 */
@Data
public class MessageSendRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 接收者用户ID
     */
    private Long toUserId;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息类型：0-文本，1-图片，2-文件
     */
    private Integer type;
}
