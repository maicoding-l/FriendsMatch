package com.mai.friendsFinder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mai.friendsFinder.model.FriendRequest;
import com.mai.friendsFinder.service.FriendRequestService;
import com.mai.friendsFinder.mapper.FriendRequestMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @description 针对表【friend_request(好友申请表)】的数据库操作Service实现
*/
@Service
public class FriendRequestServiceImpl extends ServiceImpl<FriendRequestMapper, FriendRequest>
    implements FriendRequestService{

    @Override
    public List<FriendRequest> getReceivedRequests(Long userId) {
        QueryWrapper<FriendRequest> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("to_user_id", userId);
        queryWrapper.eq("status", 0); // 只查询待处理的申请
        queryWrapper.orderByDesc("create_time");
        return this.list(queryWrapper);
    }

    @Override
    public List<FriendRequest> getSentRequests(Long userId) {
        QueryWrapper<FriendRequest> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("from_user_id", userId);
        queryWrapper.orderByDesc("create_time");
        return this.list(queryWrapper);
    }
}
