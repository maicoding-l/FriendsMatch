package com.mai.friendsFinder.controller;

import com.mai.friendsFinder.common.BaseResponse;
import com.mai.friendsFinder.common.ErrorCode;
import com.mai.friendsFinder.common.ResultUtils;
import com.mai.friendsFinder.exception.BusinessException;
import com.mai.friendsFinder.model.User;
import com.mai.friendsFinder.model.request.CommentAddRequest;
import com.mai.friendsFinder.model.vo.PostCommentVO;
import com.mai.friendsFinder.service.PostCommentService;
import com.mai.friendsFinder.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评论Controller
 */
@RestController
@RequestMapping("/comment")
@Tag(name = "评论管理")
public class PostCommentController {

    @Resource
    private PostCommentService postCommentService;

    @Resource
    private UserService userService;

    /**
     * 添加评论/回复
     */
    @Operation(summary = "添加评论")
    @PostMapping("/add")
    public BaseResponse<Long> addComment(@RequestBody CommentAddRequest commentAddRequest, HttpServletRequest request) {
        if (commentAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_AUTH, "未登录");
        }
        Long commentId = postCommentService.addComment(commentAddRequest, loginUser);
        return ResultUtils.success(commentId);
    }

    /**
     * 根据帖子ID获取评论列表
     */
    @Operation(summary = "获取帖子评论列表")
    @GetMapping("/list")
    public BaseResponse<List<PostCommentVO>> listComments(@RequestParam Long postId) {
        if (postId == null || postId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "帖子ID不正确");
        }
        List<PostCommentVO> commentList = postCommentService.listCommentsByPostId(postId);
        return ResultUtils.success(commentList);
    }

    /**
     * 删除评论
     */
    @Operation(summary = "删除评论")
    @DeleteMapping("/{id}")
    public BaseResponse<Boolean> deleteComment(@PathVariable Long id, HttpServletRequest request) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "评论ID不正确");
        }
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_AUTH, "未登录");
        }
        boolean result = postCommentService.deleteComment(id, loginUser);
        return ResultUtils.success(result);
    }
}
