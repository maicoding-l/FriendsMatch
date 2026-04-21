package com.mai.friendsFinder.handler;

import com.google.gson.Gson;
import com.mai.friendsFinder.constant.UserConstant;
import com.mai.friendsFinder.model.Message;
import com.mai.friendsFinder.model.vo.MessageVO;
import com.mai.friendsFinder.service.MessageService;
import com.mai.friendsFinder.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket聊天处理器
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    @Resource
    private MessageService messageService;

    @Resource
    private UserService userService;

    private final Gson gson = new Gson();

    // 存储用户会话：userId -> WebSocketSession
    private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = (Long) session.getAttributes().get(UserConstant.USER_LOGIN_STATE);
        if (userId != null) {
            userSessions.put(userId, session);
            System.out.println("用户 " + userId + " 已连接 WebSocket");
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long fromUserId = (Long) session.getAttributes().get(UserConstant.USER_LOGIN_STATE);
        if (fromUserId == null) {
            return;
        }

        // 解析消息
        String payload = message.getPayload();
        ChatMessage chatMessage = gson.fromJson(payload, ChatMessage.class);

        if (chatMessage == null || chatMessage.getToUserId() == null || chatMessage.getContent() == null) {
            return;
        }

        try {
            // 保存消息到数据库
            Message savedMessage = messageService.sendMessage(
                    fromUserId,
                    chatMessage.getToUserId(),
                    chatMessage.getContent(),
                    chatMessage.getType()
            );

            // 构建消息VO
            MessageVO messageVO = buildMessageVO(savedMessage);

            // 发送给接收者（如果在线）
            Long toUserId = chatMessage.getToUserId();
            WebSocketSession toUserSession = userSessions.get(toUserId);
            if (toUserSession != null && toUserSession.isOpen()) {
                String messageJson = gson.toJson(messageVO);
                toUserSession.sendMessage(new TextMessage(messageJson));
            }

            // 发送确认给发送者
            String messageJson = gson.toJson(messageVO);
            session.sendMessage(new TextMessage(messageJson));

        } catch (Exception e) {
            // 发送错误消息给发送者
            ErrorResponse errorResponse = new ErrorResponse(e.getMessage());
            session.sendMessage(new TextMessage(gson.toJson(errorResponse)));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = (Long) session.getAttributes().get(UserConstant.USER_LOGIN_STATE);
        if (userId != null) {
            userSessions.remove(userId);
            System.out.println("用户 " + userId + " 断开 WebSocket 连接");
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Long userId = (Long) session.getAttributes().get(UserConstant.USER_LOGIN_STATE);
        System.err.println("WebSocket 传输错误，用户ID: " + userId);
        exception.printStackTrace();
    }

    /**
     * 构建MessageVO
     */
    private MessageVO buildMessageVO(Message message) {
        MessageVO messageVO = new MessageVO();
        messageVO.setId(message.getId());
        messageVO.setFromUserId(message.getFromUserId());
        messageVO.setToUserId(message.getToUserId());
        messageVO.setContent(message.getContent());
        messageVO.setType(message.getType());
        messageVO.setStatus(message.getStatus());
        messageVO.setCreateTime(message.getCreateTime());

        // 获取用户信息
        var fromUser = userService.getById(message.getFromUserId());
        var toUser = userService.getById(message.getToUserId());

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

    /**
     * 聊天消息请求
     */
    private static class ChatMessage {
        private Long toUserId;
        private String content;
        private Integer type;

        public Long getToUserId() {
            return toUserId;
        }

        public void setToUserId(Long toUserId) {
            this.toUserId = toUserId;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public Integer getType() {
            return type;
        }

        public void setType(Integer type) {
            this.type = type;
        }
    }

    /**
     * 错误响应
     */
    private static class ErrorResponse {
        private String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }
}
