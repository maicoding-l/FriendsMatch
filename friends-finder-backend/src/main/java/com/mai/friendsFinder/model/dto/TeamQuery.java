package com.mai.friendsFinder.model.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.mai.friendsFinder.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
public class TeamQuery extends PageRequest {
    @Serial
    private static final long serialVersionUID = 4699110907527974645L;
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
     * 搜索关键字
     */
    private String searchText;
}
