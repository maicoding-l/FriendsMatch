package com.mai.friendsFinder;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.csv.CsvData;
import cn.hutool.core.text.csv.CsvReader;
import cn.hutool.core.text.csv.CsvRow;
import cn.hutool.core.text.csv.CsvUtil;
import com.mai.friendsFinder.model.Item;
import com.mai.friendsFinder.model.UserItem;
import com.mai.friendsFinder.service.ItemService;
import com.mai.friendsFinder.service.UserItemService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
public class ImportDataTest {

    @Resource
    private ItemService itemService; // 对应 Item 表
    @Resource
    private UserItemService userItemService; // 对应 UserItem 表

    // 这里修改为您电脑上文件的真实路径
    private static final String PATH_LINKS = "C:\\Users\\ljm\\Documents\\friendsFinder\\archive\\links_small.csv";
    private static final String PATH_METADATA = "C:\\Users\\ljm\\Documents\\friendsFinder\\archive\\movies_metadata.csv";
    private static final String PATH_RATINGS = "C:\\Users\\ljm\\Documents\\friendsFinder\\archive\\ratings_small.csv";

    // 两个核心 Map，充当“翻译官”
    // 1. Map<TMDB_ID, MovieLens_ID>
    private Map<String, Integer> tmdbToMovieLensMap = new HashMap<>();
    // 2. Map<MovieLens_ID, MySQL_Real_ID>
    private Map<Integer, Long> movieLensToMysqlIdMap = new HashMap<>();

    @Test
    public void doImport() {
        CsvReader reader = CsvUtil.getReader();

        // ==========================================
        // 阶段 1：构建桥梁 (读取 links_small.csv)
        // ==========================================
        System.out.println(">>> 1. 开始加载 ID 映射关系...");
        CsvData linksData = reader.read(FileUtil.file(PATH_LINKS), StandardCharsets.UTF_8);
        for (CsvRow row : linksData.getRows()) {
            try {
                // 表头：movieId, imdbId, tmdbId
                // 注意：Hutool 默认第一行是数据，如果第一行是表头要跳过
                if (row.getOriginalLineNumber() == 0 && row.get(0).contains("movieId")) continue;

                Integer movieLensId = Integer.parseInt(row.get(0));
                String tmdbId = row.get(2); // 第3列是 tmdbId
                if (tmdbId != null && !tmdbId.isEmpty()) {
                    tmdbToMovieLensMap.put(tmdbId, movieLensId);
                }
            } catch (Exception e) {
                // 忽略格式错误的行
            }
        }
        System.out.println(">>> ID 映射加载完成，共 " + tmdbToMovieLensMap.size() + " 条。");

        // ==========================================
        // 阶段 2：导入物品 (读取 movies_metadata.csv)
        // ==========================================
        System.out.println(">>> 2. 开始导入电影元数据 (Item表)...");
        CsvData metaData = reader.read(FileUtil.file(PATH_METADATA), StandardCharsets.UTF_8);
        int itemSuccessCount = 0;

        for (CsvRow row : metaData.getRows()) {
            try {
                // 简单判断表头
                if (row.getOriginalLineNumber() == 0) continue;

                // 获取 TMDB ID (在 movies_metadata 里叫 id)
                // 注意：metadata 的列非常多，建议用 Excel 确认 'id' 在第几列。
                // 通常 id 在第 6 列 (索引 5)，original_title 在第 9 列 (索引 8)...
                // 这里假设您已经确认了列索引，或者用 Hutool 的按名读取(需配置Header)
                // 为保险起见，建议手动数一下列索引，或者打印 row.get(5) 看看是不是数字

                String tmdbId = row.get(5); // 假设 ID 在第 6 列
                String title = row.get(8);  // 假设 title 在第 9 列
                String overview = row.get(9); // 假设 overview 在第 10 列
                String posterPath = row.get(11); // 假设 poster 在第 12 列

                // 核心判断：只有 Link 表里有的电影，才导入！
                if (tmdbToMovieLensMap.containsKey(tmdbId)) {
                    Item item = new Item();
                    item.setItemType(2); // 1-书, 2-影, 3-音 [3]
                    item.setTitle(title);

                    // 简介可能过长，如果数据库字段不够长，截断一下
                    if (overview.length() > 500) overview = overview.substring(0, 500) + "...";
                    item.setDescription(overview);

                    // 拼接海报 URL
                    if (posterPath != null && posterPath.length() > 5) {
                        item.setCoverUrl("https://image.tmdb.org/t/p/w500" + posterPath);
                    }

                    // 插入数据库
                    boolean saveResult = itemService.save(item);

                    if (saveResult) {
                        itemSuccessCount++;
                        // !!! 记录映射：MovieLens ID -> 数据库刚生成的 ID
                        Integer movieLensId = tmdbToMovieLensMap.get(tmdbId);
                        movieLensToMysqlIdMap.put(movieLensId, item.getId());
                    }
                }
            } catch (Exception e) {
                // 某些行格式可能错乱，直接跳过
            }
        }
        System.out.println(">>> 电影导入完成，成功入库: " + itemSuccessCount);

        // ==========================================
        // 阶段 3：导入评分 (读取 ratings_small.csv)
        // ==========================================
        System.out.println(">>> 3. 开始导入评分关系 (UserItem表)...");
        CsvData ratingData = reader.read(FileUtil.file(PATH_RATINGS), StandardCharsets.UTF_8);
        int relationSuccessCount = 0;

        for (CsvRow row : ratingData.getRows()) {
            try {
                if (row.getOriginalLineNumber() == 0) continue;

                // 表头：userId, movieId, rating, timestamp
                Long userId = Long.parseLong(row.get(0));
                Integer movieLensId = Integer.parseInt(row.get(1));
                Double rating = Double.parseDouble(row.get(2));

                // 查表：找到数据库里真实的 item_id
                Long realItemId = movieLensToMysqlIdMap.get(movieLensId);

                if (realItemId != null) {
                    UserItem userItem = new UserItem();
                    userItem.setUserId(userId);
                    userItem.setItemId(realItemId);
                    userItem.setWeight(rating);

                    // 设置 action [2]
                    // 4.0分以上算喜欢(1)，否则算看过(3)
                    if (rating >= 4.0) {
                        userItem.setAction(1);
                    } else {
                        userItem.setAction(3);
                    }

                    userItemService.save(userItem);
                    relationSuccessCount++;
                }
            } catch (Exception e) {
                // 忽略
            }
        }
        System.out.println(">>> 全部完成！关系数据导入: " + relationSuccessCount);
    }
}
