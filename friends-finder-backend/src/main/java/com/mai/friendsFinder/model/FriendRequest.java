package com.mai.friendsFinder.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 好友申请
 * @TableName friend_request
 */
@TableName(value ="friend_request")
@Data
public class FriendRequest {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 发起申请的用户id
     */
    private Long fromUserId;

    /**
     * 接收申请的用户id
     */
    private Long toUserId;

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
