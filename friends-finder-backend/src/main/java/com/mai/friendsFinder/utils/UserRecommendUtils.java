package com.mai.friendsFinder.utils;

import com.mai.friendsFinder.model.UserItem;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于随机游走（PersonalRank）的用户推荐算法工具类
 * 用于推荐相似用户（而非物品）
 *
 * @author ljm
 */
@Slf4j
public class UserRecommendUtils {

    /**
     * 阻尼系数（damping factor），通常取0.85
     * 表示随机游走时继续游走的概率，1-alpha表示跳回起点的概率
     */
    private static final double ALPHA = 0.85;

    /**
     * 迭代次数，通常10-20次即可收敛
     */
    private static final int MAX_ITERATIONS = 20;

    /**
     * 用户节点前缀
     */
    private static final String USER_PREFIX = "U_";

    /**
     * 物品节点前缀
     */
    private static final String ITEM_PREFIX = "I_";

    /**
     * 图结构：邻接表
     * Key: 节点ID（U_1 表示用户1，I_100 表示物品100）
     * Value: 邻居节点列表
     */
    private final Map<String, List<String>> graph;

    /**
     * 用户喜欢的物品映射
     * Key: 用户节点ID（U_1）
     * Value: 该用户喜欢的物品节点ID集合（I_100, I_200...）
     */
    private final Map<String, Set<String>> userItemsMap;

    /**
     * 构造函数，加载图数据
     *
     * @param userItems 用户-物品关系列表
     */
    public UserRecommendUtils(List<UserItem> userItems) {
        this.graph = new HashMap<>();
        this.userItemsMap = new HashMap<>();
        loadGraph(userItems);
    }

    /**
     * 1. 加载图数据 (Load Graph)
     * 从 user_item 表中读取所有数据，构建二部图
     * 逻辑：如果 userId=1 喜欢 itemId=100，则添加两条边：U_1 -> I_100 和 I_100 -> U_1
     *
     * @param userItems 用户-物品关系列表
     */
    private void loadGraph(List<UserItem> userItems) {
        if (userItems == null || userItems.isEmpty()) {
            log.warn("用户-物品关系数据为空，无法构建用户推荐图");
            return;
        }

        for (UserItem userItem : userItems) {
            Long userId = userItem.getUserId();
            Long itemId = userItem.getItemId();

            if (userId == null || itemId == null) {
                continue;
            }

            String userNode = USER_PREFIX + userId;
            String itemNode = ITEM_PREFIX + itemId;

            // 添加双向边：U_1 -> I_100
            graph.computeIfAbsent(userNode, k -> new ArrayList<>()).add(itemNode);
            // 添加双向边：I_100 -> U_1
            graph.computeIfAbsent(itemNode, k -> new ArrayList<>()).add(userNode);

            // 记录用户喜欢的物品
            userItemsMap.computeIfAbsent(userNode, k -> new HashSet<>()).add(itemNode);
        }

        log.info("用户推荐图数据加载完成，节点总数: {}, 边总数: {}",
                graph.size(),
                graph.values().stream().mapToInt(List::size).sum());
    }

    /**
     * 2. 实现随机游走 (Random Walk) - PersonalRank算法
     * 为目标用户推荐相似用户
     *
     * @param targetUserId 目标用户ID
     * @param topN         返回Top N的推荐结果
     * @return 推荐的用户ID列表（按得分降序排列）
     */
    public List<Long> getSimilarUsers(Long targetUserId, int topN) {
        if (targetUserId == null || topN <= 0) {
            log.warn("参数非法：targetUserId={}, topN={}", targetUserId, topN);
            return Collections.emptyList();
        }

        String targetUserNode = USER_PREFIX + targetUserId;

        // 检查目标用户是否在图中
        if (!graph.containsKey(targetUserNode)) {
            log.warn("用户 {} 不在图中，无法生成推荐", targetUserId);
            return Collections.emptyList();
        }

        // 初始化得分：目标用户节点得分为1.0，其他为0
        Map<String, Double> ranks = new HashMap<>();
        for (String node : graph.keySet()) {
            ranks.put(node, 0.0);
        }
        ranks.put(targetUserNode, 1.0);

        // 迭代计算（PersonalRank算法核心）
        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
            Map<String, Double> newRanks = new HashMap<>();

            // 初始化新一轮的得分
            for (String node : graph.keySet()) {
                newRanks.put(node, 0.0);
            }

            // 对每个节点，将其得分平分给邻居
            for (String node : graph.keySet()) {
                List<String> neighbors = graph.get(node);
                if (neighbors == null || neighbors.isEmpty()) {
                    continue;
                }

                double currentRank = ranks.get(node);
                int outDegree = neighbors.size();

                // 将当前节点的得分平均分配给所有邻居
                for (String neighbor : neighbors) {
                    double contribution = currentRank / outDegree;
                    newRanks.put(neighbor, newRanks.get(neighbor) + contribution);
                }
            }

            // 应用阻尼系数和重启概率
            // PR(i) = (1-α) * initial_value + α * Σ(PR(j) / OutDegree(j))
            for (String node : newRanks.keySet()) {
                double dampedRank;
                if (node.equals(targetUserNode)) {
                    // 目标用户节点：有重启概率
                    dampedRank = (1 - ALPHA) * 1.0 + ALPHA * newRanks.get(node);
                } else {
                    // 其他节点：只有游走得分
                    dampedRank = (1 - ALPHA) * 0.0 + ALPHA * newRanks.get(node);
                }
                newRanks.put(node, dampedRank);
            }

            ranks = newRanks;

            log.debug("迭代 {} 完成", iter + 1);
        }

        // 3. 过滤与排序
        return filterAndSortUsers(targetUserNode, ranks, topN);
    }

    /**
     * 3. 过滤与排序
     * 只保留用户节点（U_ 开头），剔除目标用户自己，按得分降序排序
     *
     * @param targetUserNode 目标用户节点
     * @param ranks          所有节点的得分
     * @param topN           返回Top N
     * @return 推荐的用户ID列表
     */
    private List<Long> filterAndSortUsers(String targetUserNode, Map<String, Double> ranks, int topN) {
        // 过滤并排序
        List<Long> recommendedUsers = ranks.entrySet().stream()
                // 1. 只保留用户节点（U_ 开头）
                .filter(entry -> entry.getKey().startsWith(USER_PREFIX))
                // 2. 剔除目标用户自己
                .filter(entry -> !entry.getKey().equals(targetUserNode))
                // 3. 按得分降序排序
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                // 4. 取Top N
                .limit(topN)
                // 5. 提取用户ID（去掉前缀 U_）
                .map(entry -> Long.parseLong(entry.getKey().substring(USER_PREFIX.length())))
                .collect(Collectors.toList());

        log.info("为用户 {} 生成了 {} 个相似用户推荐",
                targetUserNode.substring(USER_PREFIX.length()),
                recommendedUsers.size());

        return recommendedUsers;
    }

    /**
     * 获取推荐结果（默认返回Top 10）
     *
     * @param targetUserId 目标用户ID
     * @return 推荐的用户ID列表
     */
    public List<Long> getSimilarUsers(Long targetUserId) {
        return getSimilarUsers(targetUserId, 10);
    }

    /**
     * 获取图中的节点总数
     *
     * @return 节点总数
     */
    public int getNodeCount() {
        return graph.size();
    }

    /**
     * 获取图中的边总数
     *
     * @return 边总数
     */
    public int getEdgeCount() {
        return graph.values().stream().mapToInt(List::size).sum();
    }

    /**
     * 获取两个用户的共同喜欢物品ID列表
     *
     * @param userId1 用户1的ID
     * @param userId2 用户2的ID
     * @param topN    返回前N个共同物品
     * @return 共同物品ID列表
     */
    public List<Long> getCommonItems(Long userId1, Long userId2, int topN) {
        if (userId1 == null || userId2 == null) {
            return Collections.emptyList();
        }

        String user1Node = USER_PREFIX + userId1;
        String user2Node = USER_PREFIX + userId2;

        // 获取两个用户各自喜欢的物品
        Set<String> user1Items = userItemsMap.getOrDefault(user1Node, Collections.emptySet());
        Set<String> user2Items = userItemsMap.getOrDefault(user2Node, Collections.emptySet());

        // 计算交集（共同物品）
        Set<String> commonItemNodes = new HashSet<>(user1Items);
        commonItemNodes.retainAll(user2Items);

        // 转换为物品ID并限制数量
        return commonItemNodes.stream()
                .limit(topN)
                .map(itemNode -> Long.parseLong(itemNode.substring(ITEM_PREFIX.length())))
                .collect(Collectors.toList());
    }

    /**
     * 获取两个用户的共同喜欢物品ID列表（默认返回前5个）
     *
     * @param userId1 用户1的ID
     * @param userId2 用户2的ID
     * @return 共同物品ID列表
     */
    public List<Long> getCommonItems(Long userId1, Long userId2) {
        return getCommonItems(userId1, userId2, 5);
    }
}
