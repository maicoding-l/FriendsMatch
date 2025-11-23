package com.mai.friendsFinder.model.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class TeamUserVO implements Serializable {

    @Serial
    private static final long serialVersionUID = -4287015593771762653L;

    /**
     * id
     */
    private Long id;

    /**
     * 队伍名称
     */
    private String teamName;

    /**
     * 队伍描述
     */
    private String description;

    /**
     * 最大人数
     */
    private Integer maxNum;

    /**
     * 过期时间
     */
    private Date expireTime;

    /**
     * 队长
     */
    private Long userId;

    /**
     * 可见状态 0-公开 1-私有 2-加密
     */
    private Integer status;

    /**
     * 密码
     */
    private String password;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 创建人信息
     */
    private UserVO createUser;


    /**
     * 已加入的用户数
     */
    private Long hasJoinNum;

    /**
     * 已加入的用户ID列表（不包括队长）
     */
    private List<Long> MembersList;

    /**
     * 是否已加入队伍
     */
    private boolean hasJoin = false;


}
