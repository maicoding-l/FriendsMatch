package com.mai.friendsFinder.model.vo;

import com.mai.friendsFinder.model.Item;
import com.mai.friendsFinder.model.User;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 用户匹配结果VO
 * 包含匹配到的用户信息以及共同喜欢的物品列表
 *
 * @author ljm
 */
@Data
public class UserMatchVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 匹配到的用户
     */
    private User user;

    /**
     * 共同喜欢的物品列表（取前N个）
     */
    private List<Item> commonItems;

    /**
     * 匹配得分（可选，用于调试）
     */
    private Double score;
}
