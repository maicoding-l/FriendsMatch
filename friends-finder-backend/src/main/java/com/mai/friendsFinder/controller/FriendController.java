package com.mai.friendsFinder.controller;

import com.mai.friendsFinder.common.BaseResponse;
import com.mai.friendsFinder.common.ErrorCode;
import com.mai.friendsFinder.common.ResultUtils;
import com.mai.friendsFinder.exception.BusinessException;
import com.mai.friendsFinder.model.FriendRequest;
import com.mai.friendsFinder.model.User;
import com.mai.friendsFinder.model.request.DeleteRequest;
import com.mai.friendsFinder.model.request.FriendRequestAddRequest;
import com.mai.friendsFinder.model.request.FriendRequestHandleRequest;
import com.mai.friendsFinder.model.vo.FriendRequestVO;
import com.mai.friendsFinder.model.vo.UserVO;
import com.mai.friendsFinder.service.FriendRequestService;
import com.mai.friendsFinder.service.FriendService;
import com.mai.friendsFinder.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 好友接口
 *
 * @author ljm
 */
@RestController
@RequestMapping("/friend")
@CrossOrigin(origins = {"http://localhost:5173/"}, allowCredentials = "true")
public class FriendController {

    @Autowired
    private FriendService friendService;

    @Autowired
    private FriendRequestService friendRequestService;

    @Autowired
    private UserService userService;

    @Operation(summary = "发送好友申请")
    @PostMapping("/request/send")
    public BaseResponse<Boolean> sendFriendRequest(@RequestBody FriendRequestAddRequest request, HttpServletRequest httpRequest) {
        if (request == null || request.getToUserId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        User currentUser = userService.getLoginUser(httpRequest);
        boolean result = friendService.sendFriendRequest(request.getToUserId(), currentUser.getId(), request.getMessage());
        return ResultUtils.success(result);
    }

    @Operation(summary = "处理好友申请")
    @PostMapping("/request/handle")
    public BaseResponse<Boolean> handleFriendRequest(@RequestBody FriendRequestHandleRequest request, HttpServletRequest httpRequest) {
        if (request == null || request.getRequestId() == null || request.getAction() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        User currentUser = userService.getLoginUser(httpRequest);
        boolean result;

        if (request.getAction() == 1) {
            // 同意
            result = friendService.acceptFriendRequest(request.getRequestId(), currentUser.getId());
        } else if (request.getAction() == 2) {
            // 拒绝
            result = friendService.rejectFriendRequest(request.getRequestId(), currentUser.getId());
        } else {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "操作类型错误");
        }

        return ResultUtils.success(result);
    }

    @Operation(summary = "获取收到的好友申请")
    @GetMapping("/request/received")
    public BaseResponse<List<FriendRequestVO>> getReceivedRequests(HttpServletRequest httpRequest) {
        User currentUser = userService.getLoginUser(httpRequest);
        List<FriendRequest> requests = friendRequestService.getReceivedRequests(currentUser.getId());

        // 转换为VO，包含申请者信息
        List<FriendRequestVO> requestVOList = requests.stream().map(friendRequest -> {
            FriendRequestVO vo = new FriendRequestVO();
            vo.setId(friendRequest.getId());
            vo.setStatus(friendRequest.getStatus());
            vo.setMessage(friendRequest.getMessage());
            vo.setCreateTime(friendRequest.getCreateTime());

            // 获取发起申请的用户信息
            User fromUser = userService.getById(friendRequest.getFromUserId());
            if (fromUser != null) {
                UserVO fromUserVO = new UserVO();
                BeanUtils.copyProperties(userService.getSafetyUser(fromUser), fromUserVO);
                vo.setFromUser(fromUserVO);
            }

            return vo;
        }).collect(Collectors.toList());

        return ResultUtils.success(requestVOList);
    }

    @Operation(summary = "获取发送的好友申请")
    @GetMapping("/request/sent")
    public BaseResponse<List<FriendRequestVO>> getSentRequests(HttpServletRequest httpRequest) {
        User currentUser = userService.getLoginUser(httpRequest);
        List<FriendRequest> requests = friendRequestService.getSentRequests(currentUser.getId());

        // 转换为VO，包含接收者信息
        List<FriendRequestVO> requestVOList = requests.stream().map(friendRequest -> {
            FriendRequestVO vo = new FriendRequestVO();
            vo.setId(friendRequest.getId());
            vo.setStatus(friendRequest.getStatus());
            vo.setMessage(friendRequest.getMessage());
            vo.setCreateTime(friendRequest.getCreateTime());

            // 获取接收申请的用户信息
            User toUser = userService.getById(friendRequest.getToUserId());
            if (toUser != null) {
                UserVO toUserVO = new UserVO();
                BeanUtils.copyProperties(userService.getSafetyUser(toUser), toUserVO);
                vo.setToUser(toUserVO);
            }

            return vo;
        }).collect(Collectors.toList());

        return ResultUtils.success(requestVOList);
    }

    @Operation(summary = "获取好友列表")
    @GetMapping("/list")
    public BaseResponse<List<User>> getFriendList(HttpServletRequest httpRequest) {
        User currentUser = userService.getLoginUser(httpRequest);
        List<User> friends = friendService.getMyFriends(currentUser.getId());
        return ResultUtils.success(friends);
    }

    @Operation(summary = "删除好友")
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteFriend(@RequestBody DeleteRequest request, HttpServletRequest httpRequest) {
        if (request == null || request.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        User currentUser = userService.getLoginUser(httpRequest);
        boolean result = friendService.deleteFriend(request.getId(), currentUser.getId());
        return ResultUtils.success(result);
    }

    @Operation(summary = "检查是否是好友")
    @GetMapping("/check")
    public BaseResponse<Boolean> checkFriend(@RequestParam Long userId, HttpServletRequest httpRequest) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        User currentUser = userService.getLoginUser(httpRequest);
        boolean isFriend = friendService.isFriend(currentUser.getId(), userId);
        return ResultUtils.success(isFriend);
    }
}
