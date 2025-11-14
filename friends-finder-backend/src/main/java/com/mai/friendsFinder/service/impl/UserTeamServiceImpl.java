package com.mai.friendsFinder.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mai.friendsFinder.model.UserTeam;
import com.mai.friendsFinder.service.UserTeamService;
import com.mai.friendsFinder.mapper.UserTeamMapper;
import org.springframework.stereotype.Service;

/**
* @author ljm
* @description 针对表【user_team(用户队伍关系)】的数据库操作Service实现
* @createDate 2025-10-23 18:24:18
*/
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam>
    implements UserTeamService{

}




