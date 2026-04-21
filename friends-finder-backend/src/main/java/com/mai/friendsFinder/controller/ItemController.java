package com.mai.friendsFinder.controller;

import com.mai.friendsFinder.common.BaseResponse;
import com.mai.friendsFinder.common.ErrorCode;
import com.mai.friendsFinder.common.ResultUtils;
import com.mai.friendsFinder.exception.BusinessException;
import com.mai.friendsFinder.model.Item;
import com.mai.friendsFinder.model.request.DeleteRequest;
import com.mai.friendsFinder.model.request.ItemAddRequest;
import com.mai.friendsFinder.model.request.ItemUpdateRequest;
import com.mai.friendsFinder.model.vo.UserItemVO;
import com.mai.friendsFinder.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 物品相关接口
 *
 * @author ljm
 */
@RestController
@RequestMapping("/item")
@CrossOrigin(origins = {"http://localhost:5173/"}, allowCredentials = "true")
public class ItemController {

    @Resource
    private ItemService itemService;

    @Resource
    private com.mai.friendsFinder.service.UserItemService userItemService;

    @Resource
    private com.mai.friendsFinder.service.UserService userService;

    /**
     * 新增物品
     *
     * @param itemAddRequest 物品新增请求
     * @param request        HTTP请求用于鉴定是否为管理员
     * @return 新增物品的ID
     */
    @Operation(summary = "新增物品")
    @PostMapping("/add")
    public BaseResponse<Long> addItem(@RequestBody ItemAddRequest itemAddRequest, HttpServletRequest request) {
        if (itemAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long itemId = itemService.addItem(itemAddRequest, request);
        return ResultUtils.success(itemId);
    }

    /**
     * 删除物品
     *
     * @param deleteRequest 删除请求
     * @param request       HTTP请求用于鉴定是否为管理员
     * @return 删除是否成功
     */
    @Operation(summary = "删除物品")
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteItem(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        Boolean result = itemService.deleteItem(id, request);
        return ResultUtils.success(result);
    }

    /**
     * 修改物品信息
     *
     * @param itemUpdateRequest 物品更新请求
     * @param request           HTTP请求用于鉴定是否为管理员
     * @return 修改是否成功
     */
    @Operation(summary = "修改物品信息")
    @PostMapping("/update")
    public BaseResponse<Boolean> updateItem(@RequestBody ItemUpdateRequest itemUpdateRequest, HttpServletRequest request) {
        if (itemUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Boolean result = itemService.updateItem(itemUpdateRequest, request);
        return ResultUtils.success(result);
    }

    /**
     * 搜索物品
     *
     * @param title    标题关键词
     * @param creator  作者/导演/艺术家关键词
     * @param itemType 物品类型：1-书籍 2-电影 3-音乐
     * @param tags     标签关键词
     * @return 搜索到的物品列表
     */
    @Operation(summary = "搜索物品")
    @GetMapping("/search")
    public BaseResponse<List<Item>> searchItems(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String creator,
            @RequestParam(required = false) Integer itemType,
            @RequestParam(required = false) String tags) {
        List<Item> itemList = itemService.searchItems(title, creator, itemType, tags);
        return ResultUtils.success(itemList);
    }

    /**
     * 获取推荐物品
     *
     * @param limit 推荐数量
     * @return 推荐物品列表
     */
    @Operation(summary = "获取推荐物品")
    @GetMapping("/recommend")
    public BaseResponse<List<Item>> recommendItems(
            @RequestParam(name = "limit", defaultValue = "30") Integer limit) {
        List<Item> items = itemService.recommendItems(limit);
        return ResultUtils.success(items);
    }

    /**
     * 通过ID获取物品信息
     *
     * @param id 物品ID
     * @return 物品详情
     */
    @Operation(summary = "通过ID获取物品信息")
    @GetMapping("/getById")
    public BaseResponse<Item> getItemById(@RequestParam Long id) {
        Item item = itemService.getItemById(id);
        return ResultUtils.success(item);
    }

    /**
     * 基于随机游走算法为用户推荐物品
     *
     * @param userId 用户ID
     * @param topN   返回Top N的推荐结果（默认20，最大100）
     * @return 推荐的物品列表
     */
    @Operation(summary = "基于随机游走算法推荐物品")
    @GetMapping("/recommendByRandomWalk")
    public BaseResponse<List<Item>> recommendByRandomWalk(
            @RequestParam Long userId,
            @RequestParam(name = "topN", defaultValue = "20") Integer topN) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }
        List<Item> items = itemService.recommendItemsByRandomWalk(userId, topN);
        return ResultUtils.success(items);
    }

    /**
     * 切换收藏状态
     *
     * @param itemId  物品ID
     * @param request HTTP请求用于获取当前登录用户
     * @return 当前收藏状态（true-已收藏，false-已取消收藏）
     */
    @Operation(summary = "切换收藏状态")
    @PostMapping("/toggleFavorite")
    public BaseResponse<Boolean> toggleFavorite(
            @RequestParam Long itemId,
            HttpServletRequest request) {
        if (itemId == null || itemId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "物品ID无效");
        }

        // 获取当前登录用户
        com.mai.friendsFinder.model.User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "请先登录");
        }

        Boolean isFavorited = userItemService.toggleFavorite(loginUser.getId(), itemId);
        return ResultUtils.success(isFavorited);
    }

    /**
     * 对物品打分
     *
     * @param itemId  物品ID
     * @param score   分数 (1.0-5.0)
     * @param request HTTP请求用于获取当前登录用户
     * @return 是否成功
     */
    @Operation(summary = "对物品打分")
    @PostMapping("/rate")
    public BaseResponse<Boolean> rateItem(
            @RequestParam Long itemId,
            @RequestParam Double score,
            HttpServletRequest request) {
        if (itemId == null || itemId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "物品ID无效");
        }
        if (score == null || score < 1.0 || score > 5.0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分数必须在1.0到5.0之间");
        }

        // 获取当前登录用户
        com.mai.friendsFinder.model.User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "请先登录");
        }

        Boolean result = userItemService.rateItem(loginUser.getId(), itemId, score);
        return ResultUtils.success(result);
    }

    /**
     * 获取用户对物品的交互状态
     *
     * @param itemId  物品ID
     * @param request HTTP请求用于获取当前登录用户
     * @return UserItem对象（包含action和weight），不存在返回null
     */
    @Operation(summary = "获取用户对物品的交互状态")
    @GetMapping("/getUserAction")
    public BaseResponse<com.mai.friendsFinder.model.UserItem> getUserItemAction(
            @RequestParam Long itemId,
            HttpServletRequest request) {
        if (itemId == null || itemId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "物品ID无效");
        }

        // 获取当前登录用户
        com.mai.friendsFinder.model.User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "请先登录");
        }

        com.mai.friendsFinder.model.UserItem userItem = userItemService.getUserItemAction(loginUser.getId(), itemId);
        return ResultUtils.success(userItem);
    }

    /**
     * 获取当前用户已标记的物品列表
     *
     * @param itemType 物品类型（1-书籍 2-电影 3-音乐），不传则返回所有类型
     * @param request  HTTP请求
     * @return 已标记的物品列表
     */
    @Operation(summary = "获取用户已标记的物品列表")
    @GetMapping("/myMarked")
    public BaseResponse<List<UserItemVO>> getMyMarkedItems(
            @RequestParam(required = false) Integer itemType,
            HttpServletRequest request) {

        // 获取当前登录用户
        com.mai.friendsFinder.model.User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "请先登录");
        }

        List<UserItemVO> markedItems = userItemService.getUserMarkedItems(loginUser.getId(), itemType);
        return ResultUtils.success(markedItems);
    }
}
