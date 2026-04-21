package com.mai.friendsFinder.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 短评点赞
 * @TableName review_like
 */
@TableName(value = "review_like")
@Data
public class ReviewLike {
    /**
     * 点赞ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 短评ID
     */
    private Long reviewId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 逻辑删除标志
     */
    @TableLogic
    private Integer isDelete;
}
