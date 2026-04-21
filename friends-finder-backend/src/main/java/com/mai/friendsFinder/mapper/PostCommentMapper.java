package com.mai.friendsFinder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mai.friendsFinder.model.PostComment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评论Mapper
 */
@Mapper
public interface PostCommentMapper extends BaseMapper<PostComment> {
}
