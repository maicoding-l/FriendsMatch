package com.mai.friendsFinder.service;

import com.mai.friendsFinder.model.ItemReview;
import com.mai.friendsFinder.model.request.ItemReviewAddRequest;
import com.mai.friendsFinder.model.vo.ItemReviewVO;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 物品短评服务
 */
public interface ItemReviewService extends IService<ItemReview> {

    /**
     * 添加短评
     * @param reviewAddRequest 短评请求
     * @param request HTTP请求
     * @return 短评ID
     */
    Long addReview(ItemReviewAddRequest reviewAddRequest, HttpServletRequest request);

    /**
     * 获取物品的短评列表
     * @param itemId 物品ID
     * @param request HTTP请求（用于判断当前用户是否点赞）
     * @return 短评VO列表
     */
    List<ItemReviewVO> listReviewsByItemId(Long itemId, HttpServletRequest request);

    /**
     * 删除短评（仅作者或管理员）
     * @param reviewId 短评ID
     * @param request HTTP请求
     * @return 是否成功
     */
    Boolean deleteReview(Long reviewId, HttpServletRequest request);

    /**
     * 点赞/取消点赞短评
     * @param reviewId 短评ID
     * @param request HTTP请求
     * @return true-已点赞，false-已取消点赞
     */
    Boolean toggleLike(Long reviewId, HttpServletRequest request);
}
