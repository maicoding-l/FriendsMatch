package com.mai.friendsFinder.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mai.friendsFinder.common.BaseResponse;
import com.mai.friendsFinder.common.ErrorCode;
import com.mai.friendsFinder.common.ResultUtils;
import com.mai.friendsFinder.exception.BusinessException;
import com.mai.friendsFinder.model.Item;
import com.mai.friendsFinder.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 物品相关接口
 */
@RestController
@RequestMapping("/item")
@CrossOrigin(origins = {"http://localhost:5173/"}, allowCredentials = "true")
public class ItemController {

    @Resource
    private ItemService itemService;

    @Operation(summary = "获取推荐物品")
    @GetMapping("/recommend")
    public BaseResponse<List<Item>> recommendItems(
            @RequestParam(name = "limit", defaultValue = "30") Integer limit) {
        if (limit == null || limit <= 0 || limit > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "推荐数量范围需在 1-50 之间");
        }
        QueryWrapper<Item> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("popularity", "create_time")
                .last("limit " + limit);
        List<Item> items = itemService.list(queryWrapper);
        return ResultUtils.success(items);
    }

    @Operation(summary = "通过ID获取物品信息")
    @GetMapping("/getById")
    public BaseResponse<Item> getItemById(
            @RequestParam Long id) {
        Item item = itemService.getById(id);
        return ResultUtils.success(item);
    }
}
