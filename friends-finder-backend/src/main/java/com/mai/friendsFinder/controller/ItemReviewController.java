package com.mai.friendsFinder.controller;

import com.mai.friendsFinder.common.BaseResponse;
import com.mai.friendsFinder.common.ResultUtils;
import com.mai.friendsFinder.model.request.ItemReviewAddRequest;
import com.mai.friendsFinder.model.vo.ItemReviewVO;
import com.mai.friendsFinder.service.ItemReviewService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 物品短评控制器
 */
@RestController
@RequestMapping("/review")
@Slf4j
@CrossOrigin(origins = {"http://localhost:5173"}, allowCredentials = "true")
public class ItemReviewController {

    @Resource
    private ItemReviewService itemReviewService;

    /**
     * 添加短评
     */
    @PostMapping("/add")
    public BaseResponse<Long> addReview(@RequestBody ItemReviewAddRequest reviewAddRequest,
                                        HttpServletRequest request) {
        Long reviewId = itemReviewService.addReview(reviewAddRequest, request);
        return ResultUtils.success(reviewId);
    }

    /**
     * 获取物品的短评列表
     */
    @GetMapping("/list")
    public BaseResponse<List<ItemReviewVO>> listReviews(@RequestParam Long itemId,
                                                         HttpServletRequest request) {
        List<ItemReviewVO> reviews = itemReviewService.listReviewsByItemId(itemId, request);
        return ResultUtils.success(reviews);
    }

    /**
     * 删除短评
     */
    @DeleteMapping("/delete")
    public BaseResponse<Boolean> deleteReview(@RequestParam Long id,
                                               HttpServletRequest request) {
        Boolean result = itemReviewService.deleteReview(id, request);
        return ResultUtils.success(result);
    }

    /**
     * 点赞/取消点赞短评
     */
    @PostMapping("/like")
    public BaseResponse<Boolean> toggleLike(@RequestParam Long reviewId,
                                            HttpServletRequest request) {
        Boolean isLiked = itemReviewService.toggleLike(reviewId, request);
        return ResultUtils.success(isLiked);
    }
}
