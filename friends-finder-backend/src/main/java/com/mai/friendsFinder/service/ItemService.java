package com.mai.friendsFinder.service;

import com.mai.friendsFinder.model.Item;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mai.friendsFinder.model.request.ItemAddRequest;
import com.mai.friendsFinder.model.request.ItemUpdateRequest;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
* @author ljm
* @description 针对表【item(物品表)】的数据库操作Service
* @createDate 2025-12-17 10:12:10
*/
public interface ItemService extends IService<Item> {

    /**
     * 新增物品
     *
     * @param itemAddRequest 物品新增请求
     * @param request        HTTP请求用于鉴权
     * @return 新增物品的ID
     */
    Long addItem(ItemAddRequest itemAddRequest, HttpServletRequest request);

    /**
     * 删除物品
     *
     * @param id      物品ID
     * @param request HTTP请求用于鉴权
     * @return 删除是否成功
     */
    Boolean deleteItem(Long id, HttpServletRequest request);

    /**
     * 修改物品信息
     *
     * @param itemUpdateRequest 物品更新请求
     * @param request           HTTP请求用于鉴权
     * @return 修改是否成功
     */
    Boolean updateItem(ItemUpdateRequest itemUpdateRequest, HttpServletRequest request);

    /**
     * 搜索物品
     *
     * @param title    标题关键词
     * @param creator  作者/导演/艺术家关键词
     * @param itemType 物品类型：1-书籍 2-电影 3-音乐
     * @param tags     标签关键词
     * @return 搜索到的物品列表
     */
    List<Item> searchItems(String title, String creator, Integer itemType, String tags);

    /**
     * 获取推荐物品
     *
     * @param limit 推荐数量
     * @return 推荐物品列表
     */
    List<Item> recommendItems(Integer limit);

    /**
     * 通过ID获取物品信息
     *
     * @param id 物品ID
     * @return 物品详情
     */
    Item getItemById(Long id);

    /**
     * 基于随机游走算法为用户推荐物品
     *
     * @param userId 用户ID
     * @param topN   返回Top N的推荐结果
     * @return 推荐的物品列表
     */
    List<Item> recommendItemsByRandomWalk(Long userId, Integer topN);
}
