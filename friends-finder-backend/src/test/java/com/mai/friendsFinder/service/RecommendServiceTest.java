package com.mai.friendsFinder.service;

import com.mai.friendsFinder.mapper.UserItemMapper;
import com.mai.friendsFinder.model.UserItem;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;


@SpringBootTest
public class RecommendServiceTest {

    @Resource
    private UserItemMapper userItemMapper; // 注入您的 Mapper

    @Test
    public void testBuildGraph() {
        // 1. 从数据库拉取所有交互数据
        // 对应 user_item 表结构：user_id, item_id, weight [2]
        List<UserItem> relationList = userItemMapper.selectList(null);

        // 2. 定义图结构：邻接表
        // Map<节点ID, Map<邻居ID, 权重>>
        Map<String, Map<String, Double>> graph = new HashMap<>();

        // 3. 遍历数据构建图
        for (UserItem relation : relationList) {
            String userNode = "U_" + relation.getUserId();
            String itemNode = "I_" + relation.getItemId();
            Double weight = relation.getWeight(); // 这里的 weight 就是评分或 1.0 [2]

            // 构建正向边：User -> Item
            if (!graph.containsKey(userNode)) {
                graph.put(userNode, new HashMap<>());
            }
            graph.get(userNode).put(itemNode, weight);

            // 构建反向边：Item -> User (必须加！否则算法只能走出一步)
            if (!graph.containsKey(itemNode)) {
                graph.put(itemNode, new HashMap<>());
            }
            graph.get(itemNode).put(userNode, weight);
        }

        // 4. 验证结果
        System.out.println("图构建完成！");
        System.out.println("总节点数: " + graph.size());

        // 随便打印一个用户看看他的连接
        if (relationList.size() > 0) {
            String sampleUser = "U_" + relationList.get(92).getUserId();
            System.out.println("测试用户 " + sampleUser + " 连接了: " + graph.get(sampleUser));
        }
    }
}
