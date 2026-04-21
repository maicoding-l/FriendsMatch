package com.mai.friendsFinder.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mai.friendsFinder.model.PostComment;
import com.mai.friendsFinder.model.User;
import com.mai.friendsFinder.model.request.CommentAddRequest;
import com.mai.friendsFinder.model.vo.PostCommentVO;

import java.util.List;

/**
 * 评论Service
 */
public interface PostCommentService extends IService<PostComment> {

    /**
     * 添加评论/回复
     * @param commentAddRequest 评论请求
     * @param loginUser 当前登录用户
     * @return 评论ID
     */
    Long addComment(CommentAddRequest commentAddRequest, User loginUser);

    /**
     * 根据帖子ID获取评论列表（带层级结构）
     * @param postId 帖子ID
     * @return 评论列表
     */
    List<PostCommentVO> listCommentsByPostId(Long postId);

    /**
     * 删除评论
     * @param commentId 评论ID
     * @param loginUser 当前登录用户
     * @return 是否成功
     */
    boolean deleteComment(Long commentId, User loginUser);
}
