package com.mai.friendsFinder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mai.friendsFinder.common.ErrorCode;
import com.mai.friendsFinder.exception.BusinessException;
import com.mai.friendsFinder.model.Friend;
import com.mai.friendsFinder.model.FriendRequest;
import com.mai.friendsFinder.model.User;
import com.mai.friendsFinder.service.FriendService;
import com.mai.friendsFinder.service.FriendRequestService;
import com.mai.friendsFinder.service.UserService;
import com.mai.friendsFinder.mapper.FriendMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
* @description 针对表【friend(好友关系表)】的数据库操作Service实现
*/
@Service
public class FriendServiceImpl extends ServiceImpl<FriendMapper, Friend>
    implements FriendService{

    @Resource
    private FriendRequestService friendRequestService;

    @Resource
    private UserService userService;

    @Override
    public boolean sendFriendRequest(Long toUserId, Long currentUserId, String message) {
        // 参数校验
        if (toUserId == null || currentUserId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户id不能为空");
        }

        if (toUserId.equals(currentUserId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能向自己发送好友申请");
        }

        // 检查目标用户是否存在
        User toUser = userService.getById(toUserId);
        if (toUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "目标用户不存在");
        }

        // 检查是否已经是好友
        if (isFriend(currentUserId, toUserId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "已经是好友了");
        }

        // 检查是否已经发送过申请
        QueryWrapper<FriendRequest> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("from_user_id", currentUserId);
        queryWrapper.eq("to_user_id", toUserId);
        queryWrapper.eq("status", 0); // 待处理状态
        long count = friendRequestService.count(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "已经发送过好友申请，请等待对方处理");
        }

        // 创建好友申请
        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setFromUserId(currentUserId);
        friendRequest.setToUserId(toUserId);
        friendRequest.setMessage(message);
        friendRequest.setStatus(0); // 待处理
        friendRequest.setCreateTime(new Date());
        friendRequest.setUpdateTime(new Date());

        return friendRequestService.save(friendRequest);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean acceptFriendRequest(Long requestId, Long currentUserId) {
        // 参数校验
        if (requestId == null || currentUserId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }

        // 查询好友申请
        FriendRequest friendRequest = friendRequestService.getById(requestId);
        if (friendRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "好友申请不存在");
        }

        // 检查申请是否是发给当前用户的
        if (!friendRequest.getToUserId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限处理此申请");
        }

        // 检查申请状态
        if (friendRequest.getStatus() != 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "申请已处理");
        }

        // 更新申请状态为已同意
        friendRequest.setStatus(1);
        friendRequest.setUpdateTime(new Date());
        boolean updateResult = friendRequestService.updateById(friendRequest);
        if (!updateResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新申请状态失败");
        }

        // 创建双向好友关系
        Long fromUserId = friendRequest.getFromUserId();
        Long toUserId = friendRequest.getToUserId();

        // 创建好友关系1: from -> to
        Friend friend1 = new Friend();
        friend1.setUserId(fromUserId);
        friend1.setFriendId(toUserId);
        friend1.setCreateTime(new Date());
        friend1.setUpdateTime(new Date());

        // 创建好友关系2: to -> from
        Friend friend2 = new Friend();
        friend2.setUserId(toUserId);
        friend2.setFriendId(fromUserId);
        friend2.setCreateTime(new Date());
        friend2.setUpdateTime(new Date());

        boolean saveResult1 = this.save(friend1);
        boolean saveResult2 = this.save(friend2);

        if (!saveResult1 || !saveResult2) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建好友关系失败");
        }

        return true;
    }

    @Override
    public boolean rejectFriendRequest(Long requestId, Long currentUserId) {
        // 参数校验
        if (requestId == null || currentUserId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }

        // 查询好友申请
        FriendRequest friendRequest = friendRequestService.getById(requestId);
        if (friendRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "好友申请不存在");
        }

        // 检查申请是否是发给当前用户的
        if (!friendRequest.getToUserId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限处理此申请");
        }

        // 检查申请状态
        if (friendRequest.getStatus() != 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "申请已处理");
        }

        // 更新申请状态为已拒绝
        friendRequest.setStatus(2);
        friendRequest.setUpdateTime(new Date());
        return friendRequestService.updateById(friendRequest);
    }

    @Override
    public List<User> getMyFriends(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户id不能为空");
        }

        // 查询好友关系
        QueryWrapper<Friend> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        List<Friend> friendList = this.list(queryWrapper);

        if (friendList.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取好友id列表
        List<Long> friendIdList = friendList.stream()
                .map(Friend::getFriendId)
                .collect(Collectors.toList());

        // 批量查询好友信息
        List<User> friends = userService.listByIds(friendIdList);

        // 返回安全用户信息
        return friends.stream()
                .map(userService::getSafetyUser)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFriend(Long friendId, Long currentUserId) {
        // 参数校验
        if (friendId == null || currentUserId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }

        if (friendId.equals(currentUserId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能删除自己");
        }

        // 检查是否是好友
        if (!isFriend(currentUserId, friendId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "对方不是你的好友");
        }

        // 删除双向好友关系
        QueryWrapper<Friend> queryWrapper1 = new QueryWrapper<>();
        queryWrapper1.eq("user_id", currentUserId);
        queryWrapper1.eq("friend_id", friendId);

        QueryWrapper<Friend> queryWrapper2 = new QueryWrapper<>();
        queryWrapper2.eq("user_id", friendId);
        queryWrapper2.eq("friend_id", currentUserId);

        boolean result1 = this.remove(queryWrapper1);
        boolean result2 = this.remove(queryWrapper2);

        return result1 && result2;
    }

    @Override
    public boolean isFriend(Long userId1, Long userId2) {
        if (userId1 == null || userId2 == null) {
            return false;
        }

        QueryWrapper<Friend> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId1);
        queryWrapper.eq("friend_id", userId2);
        long count = this.count(queryWrapper);

        return count > 0;
    }
}
