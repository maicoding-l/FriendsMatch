package com.mai.friendsFinder.controller;

import com.mai.friendsFinder.common.BaseResponse;
import com.mai.friendsFinder.common.ErrorCode;
import com.mai.friendsFinder.common.ResultUtils;
import com.mai.friendsFinder.exception.BusinessException;
import com.mai.friendsFinder.model.User;
import com.mai.friendsFinder.model.vo.MessageVO;
import com.mai.friendsFinder.service.MessageService;
import com.mai.friendsFinder.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 消息接口
 */
@RestController
@RequestMapping("/message")
@CrossOrigin(origins = {"http://localhost:5173/"}, allowCredentials = "true")
public class MessageController {

    @Resource
    private MessageService messageService;

    @Resource
    private UserService userService;

    /**
     * 获取聊天历史
     */
    @Operation(summary = "获取聊天历史")
    @GetMapping("/history")
    public BaseResponse<List<MessageVO>> getChatHistory(
            @RequestParam Long targetUserId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "50") Integer pageSize,
            HttpServletRequest request) {

        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        List<MessageVO> messageList = messageService.getChatHistory(
                loginUser.getId(),
                targetUserId,
                pageNum,
                pageSize
        );

        return ResultUtils.success(messageList);
    }

    /**
     * 获取聊天列表（最近联系人）
     */
    @Operation(summary = "获取聊天列表")
    @GetMapping("/list")
    public BaseResponse<List<MessageVO>> getChatList(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        List<MessageVO> chatList = messageService.getChatList(loginUser.getId());
        return ResultUtils.success(chatList);
    }

    /**
     * 获取未读消息数
     */
    @Operation(summary = "获取未读消息数")
    @GetMapping("/unread")
    public BaseResponse<Integer> getUnreadCount(
            @RequestParam Long targetUserId,
            HttpServletRequest request) {

        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        Integer unreadCount = messageService.getUnreadCount(loginUser.getId(), targetUserId);
        return ResultUtils.success(unreadCount);
    }

    /**
     * 标记消息为已读
     */
    @Operation(summary = "标记消息为已读")
    @PostMapping("/read/{messageId}")
    public BaseResponse<Boolean> markAsRead(
            @PathVariable Long messageId,
            HttpServletRequest request) {

        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        boolean success = messageService.markAsRead(messageId, loginUser.getId());
        return ResultUtils.success(success);
    }
}
