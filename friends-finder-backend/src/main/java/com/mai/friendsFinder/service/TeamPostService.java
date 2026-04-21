package com.mai.friendsFinder.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mai.friendsFinder.model.TeamPost;
import com.mai.friendsFinder.model.User;
import com.mai.friendsFinder.model.request.PostAddRequest;
import com.mai.friendsFinder.model.vo.TeamPostVO;

import java.util.List;

/**
 * 帖子Service
 */
public interface TeamPostService extends IService<TeamPost> {

    /**
     * 发布帖子
     * @param postAddRequest 发帖请求
     * @param loginUser 当前登录用户
     * @return 帖子ID
     */
    Long addPost(PostAddRequest postAddRequest, User loginUser);

    /**
     * 根据小组ID获取帖子列表
     * @param teamId 小组ID
     * @return 帖子列表
     */
    List<TeamPostVO> listPostsByTeamId(Long teamId);

    /**
     * 根据帖子ID获取帖子详情
     * @param postId 帖子ID
     * @return 帖子详情
     */
    TeamPostVO getPostById(Long postId);

    /**
     * 删除帖子
     * @param postId 帖子ID
     * @param loginUser 当前登录用户
     * @return 是否成功
     */
    boolean deletePost(Long postId, User loginUser);

    /**
     * 增加浏览次数
     * @param postId 帖子ID
     */
    void increaseViewCount(Long postId);
}
