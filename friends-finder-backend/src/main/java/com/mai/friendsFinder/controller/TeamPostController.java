package com.mai.friendsFinder.controller;

import com.mai.friendsFinder.common.BaseResponse;
import com.mai.friendsFinder.common.ErrorCode;
import com.mai.friendsFinder.common.ResultUtils;
import com.mai.friendsFinder.exception.BusinessException;
import com.mai.friendsFinder.model.User;
import com.mai.friendsFinder.model.request.PostAddRequest;
import com.mai.friendsFinder.model.vo.TeamPostVO;
import com.mai.friendsFinder.service.TeamPostService;
import com.mai.friendsFinder.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 帖子Controller
 */
@RestController
@RequestMapping("/post")
@Tag(name = "帖子管理")
public class TeamPostController {

    @Resource
    private TeamPostService teamPostService;

    @Resource
    private UserService userService;

    /**
     * 发布帖子
     */
    @Operation(summary = "发布帖子")
    @PostMapping("/add")
    public BaseResponse<Long> addPost(@RequestBody PostAddRequest postAddRequest, HttpServletRequest request) {
        if (postAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_AUTH, "未登录");
        }
        Long postId = teamPostService.addPost(postAddRequest, loginUser);
        return ResultUtils.success(postId);
    }

    /**
     * 根据小组ID获取帖子列表
     */
    @Operation(summary = "获取小组帖子列表")
    @GetMapping("/list")
    public BaseResponse<List<TeamPostVO>> listPosts(@RequestParam Long teamId) {
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "小组ID不正确");
        }
        List<TeamPostVO> postList = teamPostService.listPostsByTeamId(teamId);
        return ResultUtils.success(postList);
    }

    /**
     * 根据帖子ID获取帖子详情
     */
    @Operation(summary = "获取帖子详情")
    @GetMapping("/{id}")
    public BaseResponse<TeamPostVO> getPost(@PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "帖子ID不正确");
        }
        // 增加浏览次数
        teamPostService.increaseViewCount(id);
        TeamPostVO post = teamPostService.getPostById(id);
        return ResultUtils.success(post);
    }

    /**
     * 删除帖子
     */
    @Operation(summary = "删除帖子")
    @DeleteMapping("/{id}")
    public BaseResponse<Boolean> deletePost(@PathVariable Long id, HttpServletRequest request) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "帖子ID不正确");
        }
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_AUTH, "未登录");
        }
        boolean result = teamPostService.deletePost(id, loginUser);
        return ResultUtils.success(result);
    }
}
