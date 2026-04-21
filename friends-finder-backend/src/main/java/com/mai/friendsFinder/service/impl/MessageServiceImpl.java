package com.mai.friendsFinder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mai.friendsFinder.common.ErrorCode;
import com.mai.friendsFinder.exception.BusinessException;
import com.mai.friendsFinder.mapper.MessageMapper;
import com.mai.friendsFinder.model.Message;
import com.mai.friendsFinder.model.User;
import com.mai.friendsFinder.model.vo.MessageVO;
import com.mai.friendsFinder.service.FriendService;
import com.mai.friendsFinder.service.MessageService;
import com.mai.friendsFinder.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @description 针对表【message(聊天消息表)】的数据库操作Service实现
 */
@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message>
        implements MessageService {

    @Resource
    private FriendService friendService;

    @Resource
    private UserService userService;

    @Override
    public Message sendMessage(Long fromUserId, Long toUserId, String content, Integer type) {
        // 参数校验
        if (fromUserId == null || toUserId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        }

        // 检查是否为好友
        if (!friendService.isFriend(fromUserId, toUserId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "只能向好友发送消息");
        }

        // 创建消息
        Message message = new Message();
        message.setFromUserId(fromUserId);
        message.setToUserId(toUserId);
        message.setContent(content);
        message.setType(type != null ? type : 0);
        message.setStatus(0); // 未读
        message.setCreateTime(new Date());
        message.setUpdateTime(new Date());

        boolean success = this.save(message);
        if (!success) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "发送消息失败");
        }

        return message;
    }

    @Override
    public List<MessageVO> getChatHistory(Long currentUserId, Long targetUserId, Integer pageNum, Integer pageSize) {
        if (currentUserId == null || targetUserId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }

        // 构建查询条件：查询两个用户之间的所有消息
        QueryWrapper<Message> queryWrapper = new QueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper
                .eq("from_user_id", currentUserId).eq("to_user_id", targetUserId)
                .or()
                .eq("from_user_id", targetUserId).eq("to_user_id", currentUserId)
        );
        queryWrapper.orderByAsc("create_time");

        // 分页查询
        Page<Message> page = new Page<>(pageNum, pageSize);
        Page<Message> messagePage = this.page(page, queryWrapper);

        // 转换为VO
        List<MessageVO> messageVOList = messagePage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return messageVOList;
    }

    @Override
    public boolean markAsRead(Long messageId, Long currentUserId) {
        if (messageId == null || currentUserId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }

        Message message = this.getById(messageId);
        if (message == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息不存在");
        }

        // 只有接收者才能标记为已读
        if (!message.getToUserId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权操作");
        }

        message.setStatus(1); // 已读
        return this.updateById(message);
    }

    @Override
    public Integer getUnreadCount(Long currentUserId, Long targetUserId) {
        if (currentUserId == null || targetUserId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }

        QueryWrapper<Message> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("from_user_id", targetUserId)
                .eq("to_user_id", currentUserId)
                .eq("status", 0); // 未读

        return Math.toIntExact(this.count(queryWrapper));
    }

    @Override
    public List<MessageVO> getChatList(Long currentUserId) {
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }

        // 查询所有与当前用户相关的消息
        QueryWrapper<Message> queryWrapper = new QueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper
                .eq("from_user_id", currentUserId)
                .or()
                .eq("to_user_id", currentUserId)
        );
        queryWrapper.orderByDesc("create_time");

        List<Message> messages = this.list(queryWrapper);

        // 按对话对象分组，每个对象只保留最后一条消息
        Map<Long, Message> lastMessageMap = new HashMap<>();
        for (Message message : messages) {
            Long otherUserId = message.getFromUserId().equals(currentUserId)
                    ? message.getToUserId()
                    : message.getFromUserId();

            if (!lastMessageMap.containsKey(otherUserId)) {
                lastMessageMap.put(otherUserId, message);
            }
        }

        // 转换为VO
        return lastMessageMap.values().stream()
                .map(this::convertToVO)
                .sorted((m1, m2) -> m2.getCreateTime().compareTo(m1.getCreateTime()))
                .collect(Collectors.toList());
    }

    /**
     * 转换为VO对象
     */
    private MessageVO convertToVO(Message message) {
        MessageVO messageVO = new MessageVO();
        BeanUtils.copyProperties(message, messageVO);

        // 获取发送者和接收者信息
        User fromUser = userService.getById(message.getFromUserId());
        User toUser = userService.getById(message.getToUserId());

        if (fromUser != null) {
            messageVO.setFromUsername(fromUser.getUsername());
            messageVO.setFromUserAvatar(fromUser.getAvatarUrl());
        }

        if (toUser != null) {
            messageVO.setToUsername(toUser.getUsername());
            messageVO.setToUserAvatar(toUser.getAvatarUrl());
        }

        return messageVO;
    }
}
