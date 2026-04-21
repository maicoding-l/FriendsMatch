package com.mai.friendsFinder.service;

import com.mai.friendsFinder.model.Friend;
import com.mai.friendsFinder.model.User;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

/**
* @description 针对表【friend(好友关系表)】的数据库操作Service
*/
public interface FriendService extends IService<Friend> {

    /**
     * 发送好友申请
     * @param toUserId 接收申请的用户id
     * @param currentUserId 当前用户id
     * @param message 申请消息
     * @return 是否成功
     */
    boolean sendFriendRequest(Long toUserId, Long currentUserId, String message);

    /**
     * 同意好友申请
     * @param requestId 申请id
     * @param currentUserId 当前用户id
     * @return 是否成功
     */
    boolean acceptFriendRequest(Long requestId, Long currentUserId);

    /**
     * 拒绝好友申请
     * @param requestId 申请id
     * @param currentUserId 当前用户id
     * @return 是否成功
     */
    boolean rejectFriendRequest(Long requestId, Long currentUserId);

    /**
     * 获取我的好友列表
     * @param userId 用户id
     * @return 好友列表（安全用户信息）
     */
    List<User> getMyFriends(Long userId);

    /**
     * 删除好友
     * @param friendId 好友id
     * @param currentUserId 当前用户id
     * @return 是否成功
     */
    boolean deleteFriend(Long friendId, Long currentUserId);

    /**
     * 检查是否是好友
     * @param userId1 用户1
     * @param userId2 用户2
     * @return 是否是好友
     */
    boolean isFriend(Long userId1, Long userId2);
}
