package com.mai.friendsFinder.service.impl;

import com.mai.friendsFinder.service.RecommendService;

import java.util.List;

public class RecommendServiceImpl implements RecommendService {

    /**
     * 核心算法：PersonalRank
     * @param targetUserId 目标用户ID (如 2)
     * @return 推荐的物品ID列表
     */
    @Override
    public List<Long> personalRank(Long targetUserId) {
        // 1. 转换ID：把 Long 2 变成 "U_2"
        String startNode = "U_" + targetUserId;

        // 2. 初始化：除了 startNode 是 1.0，其他节点都是 0
        //    (此处省略 20 次循环迭代的代码...)

        // 3. 排序：取出所有 "I_" 开头的节点，按分数降序排列

        // 4. 过滤：去掉用户已经看过的 (也就是 graph.get("U_2") 里已经有的那些 I_xxx)

        // 5. 返回前 10 个物品的 ID
        return List.of();
    }
}
