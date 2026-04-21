package com.mai.friendsFinder.service;

import com.mai.friendsFinder.model.FriendRequest;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

/**
* @description 针对表【friend_request(好友申请表)】的数据库操作Service
*/
public interface FriendRequestService extends IService<FriendRequest> {

    /**
     * 获取我收到的好友申请
     * @param userId 用户id
     * @return 申请列表
     */
    List<FriendRequest> getReceivedRequests(Long userId);

    /**
     * 获取我发送的好友申请
     * @param userId 用户id
     * @return 申请列表
     */
    List<FriendRequest> getSentRequests(Long userId);
}
