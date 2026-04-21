package com.mai.friendsFinder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mai.friendsFinder.common.ErrorCode;
import com.mai.friendsFinder.exception.BusinessException;
import com.mai.friendsFinder.mapper.ItemReviewMapper;
import com.mai.friendsFinder.mapper.ReviewLikeMapper;
import com.mai.friendsFinder.model.ItemReview;
import com.mai.friendsFinder.model.ReviewLike;
import com.mai.friendsFinder.model.User;
import com.mai.friendsFinder.model.request.ItemReviewAddRequest;
import com.mai.friendsFinder.model.vo.ItemReviewVO;
import com.mai.friendsFinder.service.ItemReviewService;
import com.mai.friendsFinder.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 物品短评服务实现
 */
@Service
@Slf4j
public class ItemReviewServiceImpl extends ServiceImpl<ItemReviewMapper, ItemReview>
        implements ItemReviewService {

    @Resource
    private UserService userService;

    @Resource
    private ReviewLikeMapper reviewLikeMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addReview(ItemReviewAddRequest reviewAddRequest, HttpServletRequest request) {
        if (reviewAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Long itemId = reviewAddRequest.getItemId();
        String content = reviewAddRequest.getContent();
        Double rating = reviewAddRequest.getRating();

        // 校验参数
        if (itemId == null || itemId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "物品ID无效");
        }
        if (StringUtils.isBlank(content)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "短评内容不能为空");
        }
        if (content.length() > 500) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "短评内容不能超过500字");
        }
        if (rating != null && (rating < 1.0 || rating > 5.0)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "评分必须在1.0到5.0之间");
        }

        // 获取当前用户
        User loginUser = userService.getLoginUser(request);

        // 检查用户是否已经对该物品发表过短评
        QueryWrapper<ItemReview> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", loginUser.getId()).eq("item_id", itemId);
        ItemReview existingReview = this.getOne(queryWrapper);

        if (existingReview != null) {
            // 更新短评
            existingReview.setContent(content);
            existingReview.setRating(rating);
            boolean result = this.updateById(existingReview);
            if (!result) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新短评失败");
            }
            log.info("用户 {} 更新了物品 {} 的短评", loginUser.getId(), itemId);
            return existingReview.getId();
        } else {
            // 新增短评
            ItemReview review = new ItemReview();
            review.setUserId(loginUser.getId());
            review.setItemId(itemId);
            review.setContent(content);
            review.setRating(rating);
            review.setLikeCount(0);

            boolean result = this.save(review);
            if (!result) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "添加短评失败");
            }
            log.info("用户 {} 对物品 {} 发表了短评", loginUser.getId(), itemId);
            return review.getId();
        }
    }

    @Override
    public List<ItemReviewVO> listReviewsByItemId(Long itemId, HttpServletRequest request) {
        if (itemId == null || itemId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "物品ID无效");
        }

        // 获取当前用户（可能未登录）
        User loginUser = null;
        try {
            loginUser = userService.getLoginUser(request);
        } catch (Exception e) {
            // 未登录，继续执行
        }

        // 查询该物品的所有短评
        QueryWrapper<ItemReview> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("item_id", itemId);
        queryWrapper.orderByDesc("like_count", "create_time");
        List<ItemReview> reviews = this.list(queryWrapper);

        if (reviews.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取所有短评的用户ID
        List<Long> userIds = reviews.stream()
                .map(ItemReview::getUserId)
                .distinct()
                .collect(Collectors.toList());

        // 批量查询用户信息
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.in("id", userIds);
        Map<Long, User> userMap = userService.list(userQueryWrapper).stream()
                .map(userService::getSafetyUser)
                .collect(Collectors.toMap(User::getId, user -> user));

        // 如果用户已登录，查询用户点赞的短评ID
        Set<Long> likedReviewIds = null;
        if (loginUser != null) {
            QueryWrapper<ReviewLike> likeQueryWrapper = new QueryWrapper<>();
            likeQueryWrapper.eq("user_id", loginUser.getId());
            likeQueryWrapper.in("review_id", reviews.stream().map(ItemReview::getId).collect(Collectors.toList()));
            likedReviewIds = reviewLikeMapper.selectList(likeQueryWrapper).stream()
                    .map(ReviewLike::getReviewId)
                    .collect(Collectors.toSet());
        }

        // 构建VO列表
        List<ItemReviewVO> reviewVOList = new ArrayList<>();
        for (ItemReview review : reviews) {
            ItemReviewVO vo = new ItemReviewVO();
            vo.setId(review.getId());
            vo.setUser(userMap.get(review.getUserId()));
            vo.setItemId(review.getItemId());
            vo.setContent(review.getContent());
            vo.setRating(review.getRating());
            vo.setLikeCount(review.getLikeCount());
            vo.setCreateTime(review.getCreateTime());

            // 设置是否点赞
            if (likedReviewIds != null) {
                vo.setIsLiked(likedReviewIds.contains(review.getId()));
            } else {
                vo.setIsLiked(false);
            }

            reviewVOList.add(vo);
        }

        return reviewVOList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteReview(Long reviewId, HttpServletRequest request) {
        if (reviewId == null || reviewId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "短评ID无效");
        }

        // 获取当前用户
        User loginUser = userService.getLoginUser(request);

        // 查询短评
        ItemReview review = this.getById(reviewId);
        if (review == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "短评不存在");
        }

        // 权限校验：只有作者或管理员可以删除
        if (!review.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权删除此短评");
        }

        boolean result = this.removeById(reviewId);
        if (result) {
            log.info("用户 {} 删除了短评 {}", loginUser.getId(), reviewId);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean toggleLike(Long reviewId, HttpServletRequest request) {
        if (reviewId == null || reviewId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "短评ID无效");
        }

        // 获取当前用户
        User loginUser = userService.getLoginUser(request);

        // 查询短评是否存在
        ItemReview review = this.getById(reviewId);
        if (review == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "短评不存在");
        }

        // 查询是否已点赞
        QueryWrapper<ReviewLike> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", loginUser.getId()).eq("review_id", reviewId);
        ReviewLike existingLike = reviewLikeMapper.selectOne(queryWrapper);

        if (existingLike != null) {
            // 取消点赞
            boolean deleteResult = reviewLikeMapper.deleteById(existingLike.getId()) > 0;
            if (!deleteResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "取消点赞失败");
            }

            // 减少点赞数
            review.setLikeCount(Math.max(0, review.getLikeCount() - 1));
            boolean updateResult = this.updateById(review);
            if (!updateResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新点赞数失败");
            }

            log.info("用户 {} 取消点赞短评 {}", loginUser.getId(), reviewId);
            return false;
        } else {
            // 点赞
            ReviewLike like = new ReviewLike();
            like.setUserId(loginUser.getId());
            like.setReviewId(reviewId);

            int insertResult = reviewLikeMapper.insert(like);
            if (insertResult <= 0) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "点赞失败");
            }

            // 增加点赞数
            review.setLikeCount(review.getLikeCount() + 1);
            boolean updateResult = this.updateById(review);
            if (!updateResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新点赞数失败");
            }

            log.info("用户 {} 点赞短评 {}", loginUser.getId(), reviewId);
            return true;
        }
    }
}
