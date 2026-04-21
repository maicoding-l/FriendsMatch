package com.mai.friendsFinder.service;

import com.mai.friendsFinder.model.Message;
import com.mai.friendsFinder.model.vo.MessageVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @description 针对表【message(聊天消息表)】的数据库操作Service
 */
public interface MessageService extends IService<Message> {

    /**
     * 发送消息
     * @param fromUserId 发送者ID
     * @param toUserId 接收者ID
     * @param content 消息内容
     * @param type 消息类型
     * @return 消息对象
     */
    Message sendMessage(Long fromUserId, Long toUserId, String content, Integer type);

    /**
     * 获取与某个用户的聊天记录
     * @param currentUserId 当前用户ID
     * @param targetUserId 目标用户ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 消息列表
     */
    List<MessageVO> getChatHistory(Long currentUserId, Long targetUserId, Integer pageNum, Integer pageSize);

    /**
     * 标记消息为已读
     * @param messageId 消息ID
     * @param currentUserId 当前用户ID
     * @return 是否成功
     */
    boolean markAsRead(Long messageId, Long currentUserId);

    /**
     * 获取与某个用户的未读消息数
     * @param currentUserId 当前用户ID
     * @param targetUserId 目标用户ID
     * @return 未读消息数
     */
    Integer getUnreadCount(Long currentUserId, Long targetUserId);

    /**
     * 获取所有聊天会话列表（最近联系人）
     * @param currentUserId 当前用户ID
     * @return 最近联系人列表及最后一条消息
     */
    List<MessageVO> getChatList(Long currentUserId);
}
