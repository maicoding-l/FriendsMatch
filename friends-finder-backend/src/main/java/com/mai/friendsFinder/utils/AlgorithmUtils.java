package com.mai.friendsFinder.utils;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class AlgorithmUtils {
    /**
     * 编辑距离算法（用于计算最相似的两组标签）
     *
     * @param tagList1
     * @param tagList2
     * @return
     */
    public static int minDistance(List<String> tagList1, List<String> tagList2) {
        Set<String> set1 = new HashSet<>(tagList1);
        Set<String> set2 = new HashSet<>(tagList2);

        // 交集
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        int common = intersection.size();
        int total = set1.size() + set2.size();

        // 对称差集大小 = |A| + |B| - 2 * |A ∩ B|
        return (int) (total - 2L * common);
    }


    /**
     * Jaccard 相似度计算
     * @param tagList1
     * @param tagList2
     * @return
     */
    public static double calcSimilarity(List<String> tagList1, List<String> tagList2) {
        if (tagList1 == null || tagList2 == null || tagList1.isEmpty() || tagList2.isEmpty()) {
            // 只要有一边没标签，相似度统一按 0 处理
            return 0.0;
        }

        // 统一做一下清洗：去掉前后空格、去除 null
        Set<String> set1 = tagList1.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        Set<String> set2 = tagList2.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        if (set1.isEmpty() || set2.isEmpty()) {
            return 0.0;
        }

        // 交集
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        // 并集
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        if (union.isEmpty()) {
            return 0.0;
        }

        // Jaccard 相似度
        return (double) intersection.size() / union.size();
    }




}
