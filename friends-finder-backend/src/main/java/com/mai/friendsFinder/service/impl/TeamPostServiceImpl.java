package com.mai.friendsFinder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mai.friendsFinder.common.ErrorCode;
import com.mai.friendsFinder.exception.BusinessException;
import com.mai.friendsFinder.mapper.TeamPostMapper;
import com.mai.friendsFinder.model.TeamPost;
import com.mai.friendsFinder.model.User;
import com.mai.friendsFinder.model.UserTeam;
import com.mai.friendsFinder.model.request.PostAddRequest;
import com.mai.friendsFinder.model.vo.TeamPostVO;
import com.mai.friendsFinder.model.vo.UserVO;
import com.mai.friendsFinder.service.TeamPostService;
import com.mai.friendsFinder.service.UserService;
import com.mai.friendsFinder.service.UserTeamService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 帖子Service实现
 */
@Service
public class TeamPostServiceImpl extends ServiceImpl<TeamPostMapper, TeamPost> implements TeamPostService {

    @Resource
    private UserService userService;

    @Resource
    private UserTeamService userTeamService;

    @Override
    public Long addPost(PostAddRequest postAddRequest, User loginUser) {
        if (postAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_AUTH, "未登录");
        }

        Long teamId = postAddRequest.getTeamId();
        String title = postAddRequest.getTitle();
        String content = postAddRequest.getContent();

        // 校验参数
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "小组ID不正确");
        }
        if (StringUtils.isBlank(title) || title.length() > 256) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标题不符合要求");
        }
        if (StringUtils.isBlank(content)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容不能为空");
        }

        // 校验用户是否已加入该小组
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("TEAM_ID", teamId);
        queryWrapper.eq("USER_ID", loginUser.getId());
        long count = userTeamService.count(queryWrapper);
        if (count == 0) {
            throw new BusinessException(ErrorCode.NO_AUTH, "未加入该小组，无法发帖");
        }

        // 创建帖子
        TeamPost post = new TeamPost();
        post.setTeamId(teamId);
        post.setUserId(loginUser.getId());
        post.setTitle(title);
        post.setContent(content);
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setCommentCount(0);

        boolean result = this.save(post);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "发帖失败");
        }

        return post.getId();
    }

    @Override
    public List<TeamPostVO> listPostsByTeamId(Long teamId) {
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "小组ID不正确");
        }

        QueryWrapper<TeamPost> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("TEAM_ID", teamId);
        queryWrapper.orderByDesc("CREATE_TIME");

        List<TeamPost> postList = this.list(queryWrapper);
        List<TeamPostVO> postVOList = new ArrayList<>();

        for (TeamPost post : postList) {
            TeamPostVO postVO = new TeamPostVO();
            BeanUtils.copyProperties(post, postVO);

            // 获取发帖用户信息
            User user = userService.getById(post.getUserId());
            if (user != null) {
                UserVO userVO = new UserVO();
                BeanUtils.copyProperties(userService.getSafetyUser(user), userVO);
                postVO.setUser(userVO);
            }

            postVOList.add(postVO);
        }

        return postVOList;
    }

    @Override
    public TeamPostVO getPostById(Long postId) {
        if (postId == null || postId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "帖子ID不正确");
        }

        TeamPost post = this.getById(postId);
        if (post == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "帖子不存在");
        }

        TeamPostVO postVO = new TeamPostVO();
        BeanUtils.copyProperties(post, postVO);

        // 获取发帖用户信息
        User user = userService.getById(post.getUserId());
        if (user != null) {
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(userService.getSafetyUser(user), userVO);
            postVO.setUser(userVO);
        }

        return postVO;
    }

    @Override
    public boolean deletePost(Long postId, User loginUser) {
        if (postId == null || postId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "帖子ID不正确");
        }
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_AUTH, "未登录");
        }

        TeamPost post = this.getById(postId);
        if (post == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "帖子不存在");
        }

        // 只有作者或管理员可以删除
        if (!post.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权删除该帖子");
        }

        return this.removeById(postId);
    }

    @Override
    public void increaseViewCount(Long postId) {
        if (postId == null || postId <= 0) {
            return;
        }

        UpdateWrapper<TeamPost> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("ID", postId);
        updateWrapper.setSql("view_count = view_count + 1");
        this.update(updateWrapper);
    }
}
