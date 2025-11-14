package com.mai.friendsFinder.service;

import com.mai.friendsFinder.model.Team;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mai.friendsFinder.model.User;
import com.mai.friendsFinder.model.dto.TeamQuery;
import com.mai.friendsFinder.model.request.TeamJoinRequest;
import com.mai.friendsFinder.model.request.TeamQuitRequest;
import com.mai.friendsFinder.model.request.TeamUpdateRequest;
import com.mai.friendsFinder.model.vo.TeamUserVO;

import java.util.List;

/**
* @author ljm
* @description 针对表【team(队伍)】的数据库操作Service
* @createDate 2025-10-23 18:24:08
*/
public interface TeamService extends IService<Team> {
    /**
     * 新增队伍
     */
    public long addTeam(Team team, User loginUser);

    /**
     * 查询队伍列表
     * @param teamQuery
     * @param isAdmin
     * @return
     */
    public List<TeamUserVO> listTeams(TeamQuery teamQuery, boolean isAdmin);

    /**
     * 队伍信息更新
     * @param teamUpdateRequest
     * @param loginUser
     * @return
     */
    public boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser);

    /**
     * 加入队伍
     * @param teamJoinRequest
     * @param loginUser
     * @return
     */
    public boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser);


    /**
     * 退出队伍
     * @param teamQuitRequest
     * @param loginUser
     * @return
     */
    public boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser);


    /**
     * 删除队伍
     * @param id
     * @param loginUser
     * @return
     */
    public boolean deleteTeam(long id, User loginUser);

}
