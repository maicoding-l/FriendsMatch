package com.mai.friendsFinder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mai.friendsFinder.common.ErrorCode;
import com.mai.friendsFinder.exception.BusinessException;
import com.mai.friendsFinder.mapper.PostCommentMapper;
import com.mai.friendsFinder.model.PostComment;
import com.mai.friendsFinder.model.TeamPost;
import com.mai.friendsFinder.model.User;
import com.mai.friendsFinder.model.request.CommentAddRequest;
import com.mai.friendsFinder.model.vo.PostCommentVO;
import com.mai.friendsFinder.model.vo.UserVO;
import com.mai.friendsFinder.service.PostCommentService;
import com.mai.friendsFinder.service.TeamPostService;
import com.mai.friendsFinder.service.UserService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 评论Service实现
 */
@Service
public class PostCommentServiceImpl extends ServiceImpl<PostCommentMapper, PostComment> implements PostCommentService {

    @Resource
    private UserService userService;

    @Resource
    private TeamPostService teamPostService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addComment(CommentAddRequest commentAddRequest, User loginUser) {
        if (commentAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_AUTH, "未登录");
        }

        Long postId = commentAddRequest.getPostId();
        String content = commentAddRequest.getContent();
        Long parentId = commentAddRequest.getParentId();
        Long replyToUserId = commentAddRequest.getReplyToUserId();

        // 校验参数
        if (postId == null || postId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "帖子ID不正确");
        }
        if (StringUtils.isBlank(content)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "评论内容不能为空");
        }

        // 校验帖子是否存在
        TeamPost post = teamPostService.getById(postId);
        if (post == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "帖子不存在");
        }

        // 创建评论
        PostComment comment = new PostComment();
        comment.setPostId(postId);
        comment.setUserId(loginUser.getId());
        comment.setContent(content);
        comment.setParentId(parentId);
        comment.setReplyToUserId(replyToUserId);
        comment.setLikeCount(0);

        boolean result = this.save(comment);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "评论失败");
        }

        // 更新帖子评论数
        UpdateWrapper<TeamPost> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("ID", postId);
        updateWrapper.setSql("comment_count = comment_count + 1");
        teamPostService.update(updateWrapper);

        return comment.getId();
    }

    @Override
    public List<PostCommentVO> listCommentsByPostId(Long postId) {
        if (postId == null || postId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "帖子ID不正确");
        }

        QueryWrapper<PostComment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("POST_ID", postId);
        queryWrapper.orderByAsc("CREATE_TIME");

        List<PostComment> commentList = this.list(queryWrapper);
        List<PostCommentVO> commentVOList = new ArrayList<>();

        // 转换为VO并添加用户信息
        for (PostComment comment : commentList) {
            PostCommentVO commentVO = new PostCommentVO();
            BeanUtils.copyProperties(comment, commentVO);

            // 获取评论用户信息
            User user = userService.getById(comment.getUserId());
            if (user != null) {
                UserVO userVO = new UserVO();
                BeanUtils.copyProperties(userService.getSafetyUser(user), userVO);
                commentVO.setUser(userVO);
            }

            // 获取回复对象用户信息
            if (comment.getReplyToUserId() != null) {
                User replyToUser = userService.getById(comment.getReplyToUserId());
                if (replyToUser != null) {
                    UserVO replyToUserVO = new UserVO();
                    BeanUtils.copyProperties(userService.getSafetyUser(replyToUser), replyToUserVO);
                    commentVO.setReplyToUser(replyToUserVO);
                }
            }

            commentVOList.add(commentVO);
        }

        // 构建层级结构（一级评论和二级回复）
        return buildCommentTree(commentVOList);
    }

    /**
     * 构建评论树形结构
     */
    private List<PostCommentVO> buildCommentTree(List<PostCommentVO> allComments) {
        // 找出所有一级评论（parentId为null）
        List<PostCommentVO> rootComments = allComments.stream()
                .filter(comment -> comment.getParentId() == null)
                .collect(Collectors.toList());

        // 将评论按照ID分组，方便查找
        Map<Long, PostCommentVO> commentMap = allComments.stream()
                .collect(Collectors.toMap(PostCommentVO::getId, comment -> comment));

        // 为每个一级评论找到它的所有回复
        for (PostCommentVO rootComment : rootComments) {
            List<PostCommentVO> replies = allComments.stream()
                    .filter(comment -> comment.getParentId() != null &&
                            (comment.getParentId().equals(rootComment.getId()) ||
                             isChildOf(comment, rootComment.getId(), commentMap)))
                    .collect(Collectors.toList());
            rootComment.setReplies(replies);
        }

        return rootComments;
    }

    /**
     * 判断评论是否是某个父评论的子孙
     */
    private boolean isChildOf(PostCommentVO comment, Long ancestorId, Map<Long, PostCommentVO> commentMap) {
        Long parentId = comment.getParentId();
        while (parentId != null) {
            if (parentId.equals(ancestorId)) {
                return true;
            }
            PostCommentVO parent = commentMap.get(parentId);
            if (parent == null) {
                break;
            }
            parentId = parent.getParentId();
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteComment(Long commentId, User loginUser) {
        if (commentId == null || commentId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "评论ID不正确");
        }
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_AUTH, "未登录");
        }

        PostComment comment = this.getById(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "评论不存在");
        }

        // 只有作者或管理员可以删除
        if (!comment.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权删除该评论");
        }

        boolean result = this.removeById(commentId);

        // 更新帖子评论数
        if (result) {
            UpdateWrapper<TeamPost> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("ID", comment.getPostId());
            updateWrapper.setSql("comment_count = comment_count - 1");
            teamPostService.update(updateWrapper);
        }

        return result;
    }
}
