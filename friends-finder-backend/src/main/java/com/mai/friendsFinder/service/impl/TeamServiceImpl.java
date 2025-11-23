package com.mai.friendsFinder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mai.friendsFinder.common.ErrorCode;
import com.mai.friendsFinder.exception.BusinessException;
import com.mai.friendsFinder.model.Team;
import com.mai.friendsFinder.model.User;
import com.mai.friendsFinder.model.UserTeam;
import com.mai.friendsFinder.model.dto.TeamQuery;
import com.mai.friendsFinder.model.enums.TeamStatusEnum;
import com.mai.friendsFinder.model.request.TeamJoinRequest;
import com.mai.friendsFinder.model.request.TeamQuitRequest;
import com.mai.friendsFinder.model.request.TeamUpdateRequest;
import com.mai.friendsFinder.model.vo.TeamUserVO;
import com.mai.friendsFinder.model.vo.UserVO;
import com.mai.friendsFinder.service.TeamService;
import com.mai.friendsFinder.mapper.TeamMapper;
import com.mai.friendsFinder.service.UserService;
import com.mai.friendsFinder.service.UserTeamService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.awt.dnd.DropTargetEvent;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


/**
* @author ljm
* @description 针对表【team(队伍)】的数据库操作Service实现
* @createDate 2025-10-23 18:24:08
*/
@Transactional(rollbackFor = Exception.class)
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
    implements TeamService{
    @Resource
    private UserTeamService userTeamService;

    @Resource
    private UserService userService;

    @Override
    public long addTeam(Team team, User loginUser) {
    //        1. 请求参数是否为空？
        if(team==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"参数为空");
        }
    //        2. 是否登录，未登录不允许创建
        if(loginUser==null){
            throw new BusinessException(ErrorCode.NO_AUTH,"未登录");
        }
        final long userId = loginUser.getId();
    //        3. 校验信息
    //        a. 队伍人数 > 1 且 <= 20
        int maxNum = Optional.ofNullable(team.getMaxNum()).orElse(0);
        if(maxNum<1||maxNum>20){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍人数不合规定");
        }
    //        b. 队伍标题 <= 20
        String name = team.getTeamName();
        if(StringUtils.isBlank(name)||name.length()>20){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍名称不符合规定");
        }
    //        c. 描述 <= 512
        String description = team.getDescription();
        if(StringUtils.isBlank(description)||description.length()>512){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍简介不符合规定");
        }
    //        d. status 是否公开（int）不传默认为 0（公开）
        int status = Optional.ofNullable(team.getStatus()).orElse(0);
        TeamStatusEnum statusEnum = TeamStatusEnum.getTeamStatusEnumByValue(status);
        if(statusEnum==null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍状态参数不合规定");
        }
    //        e. 如果 status 是加密状态，一定要有密码，且密码 <= 32
        String password = team.getPassword();
        if(StringUtils.isBlank(password)||password.length()>32){
            if(TeamStatusEnum.SECRET.equals(statusEnum)){
                if(StringUtils.isBlank(password)||password.length()>32){
                    throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码设置不正确");
                }
            }
        }

    //        f. 超时时间 > 当前时间
        Date expireTime = team.getExpireTime();
        if(new Date().after(expireTime)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"过期日设置不正确");
        }
    //        g. 校验用户最多创建 5 个队伍
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("USER_ID",userId);
        long count = this.count(queryWrapper);
        if(count>=6){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户最多创建5个队伍");
        }
    //        4. 插入队伍信息到队伍表
        team.setId(null);
        team.setUserId(userId);
        boolean result = this.save(team);
        Long teamId = team.getId();
        if(!result){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"新建队伍失败");
        }
    //        5. 插入用户 => 队伍关系到关系表
        UserTeam userTeam = new UserTeam();
        userTeam.setTeamId(teamId);
        userTeam.setUserId(userId);
        userTeam.setJoinTime(new Date());
        result = userTeamService.save(userTeam);
        if(!result){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"创建用户队伍关系失败");
        }
        return teamId;
    }


    /**
     * 查询队伍
     * @param teamQuery
     * @param isAdmin
     * @return
     */
    @Override
    public List<TeamUserVO> listTeams(TeamQuery teamQuery, boolean isAdmin) {
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        if(teamQuery!=null){
            Long id = teamQuery.getId();
            if(id!=null&&id>0){
                queryWrapper.eq("ID",id);
            }
            //查询加入队伍
            List<Long> idList = teamQuery.getIdList();
            if(!CollectionUtils.isEmpty(idList)){
                queryWrapper.in("ID",idList);
            }
            //按searchText查询名称或描述
            String searchText = teamQuery.getSearchText();
            if(StringUtils.isNotBlank(searchText)){
                queryWrapper.like("TEAM_NAME",searchText).or().like("DESCRIPTION",searchText).or().like("EXPIRE_TIME",searchText);
            }
            //按队伍名字查询
            String name = teamQuery.getTeamName();
            if(StringUtils.isNotBlank(name)){
                queryWrapper.like("TEAM_NAME",name);
            }
            //按描述查询
            String description = teamQuery.getDescription();
            if(StringUtils.isNotBlank(description)){
                queryWrapper.like("DESCRIPTION",description);
            }
            //按最大人数查询
            Integer maxNum = teamQuery.getMaxNum();
            if(maxNum!=null&&maxNum>0){
                queryWrapper.eq("MAX_NUM",maxNum);
            }
            //按队长来搜索队伍
            Long userId = teamQuery.getUserId();
            if(userId!=null&&userId>0){
                queryWrapper.eq("USER_ID",userId);
            }
            //根据状态查询
            Integer status = teamQuery.getStatus();
            TeamStatusEnum statusEnum = TeamStatusEnum.getTeamStatusEnumByValue(status);
            if(statusEnum==null){
                statusEnum = TeamStatusEnum.PUBLIC;
            }
            if(!isAdmin&&!statusEnum.equals(TeamStatusEnum.PUBLIC)){
                throw new BusinessException(ErrorCode.NO_AUTH);
            }
            queryWrapper.eq("STATUS",statusEnum.getValue());
        }
        //不展示已过期的队伍
        queryWrapper.and(qw->qw.gt("expire_time",new Date()).or().isNull("expire_time"));

        List<Team> teamList = this.list(queryWrapper);
        if(CollectionUtils.isEmpty(teamList)){
            return new ArrayList<>();
        }
        List<TeamUserVO> teamUserVOList = new ArrayList<>();
        //查询创始人相关的用户信息和成员ID信息
        for(Team team:teamList){
            Long userId = team.getUserId();
            if (userId == null) {
                continue;
            }
            User user = userService.getById(userId);
            TeamUserVO teamUserVO = new TeamUserVO();
            BeanUtils.copyProperties(team,teamUserVO);
            //设置加入队伍人数
            Long TeamId = team.getId();
            Long hasJoinNum = this.countTeamUserByTeamId(TeamId);
            teamUserVO.setHasJoinNum(hasJoinNum);
            //设置加入队伍人员ID
            List<Long> membersId = userTeamService.lambdaQuery()
                    .select(UserTeam::getUserId)
                    .eq(UserTeam::getTeamId, TeamId)
                    .list()
                    .stream()
                    .map(UserTeam::getUserId)
                    .collect(Collectors.toList());
            teamUserVO.setMembersList(membersId);
            //脱敏用户信息
            if(user!=null){
                UserVO userVO = new UserVO();
                BeanUtils.copyProperties(user,userVO);
                teamUserVO.setCreateUser(userVO);
            }
            teamUserVOList.add(teamUserVO);
        }
        return teamUserVOList;
    }


    /**
     * 队伍信息更新
     * @param teamUpdateRequest
     * @param loginUser
     * @return
     */
    @Override
    public boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser) {
        if (teamUpdateRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = teamUpdateRequest.getId();
        if(id==null&&id<0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team oldTeam = this.getById(id);
        if(oldTeam==null){
            throw new BusinessException(ErrorCode.NULL_ERROR,"要修改的队伍不存在");
        }
        if(oldTeam.getUserId()!= loginUser.getId()&&!userService.isAdmin(loginUser)){
            throw new BusinessException(ErrorCode.NO_AUTH,"不是队长或不是管理员无权修改队伍");
        }
        //如果修改队伍状态为加密，必须要输入密码
        Integer status = teamUpdateRequest.getStatus();
        if(status!=null){
            TeamStatusEnum statusEnum = TeamStatusEnum.getTeamStatusEnumByValue(status);
            if (statusEnum.equals(TeamStatusEnum.SECRET)) {
                String password = teamUpdateRequest.getPassword();
                if(StringUtils.isBlank(password)){
                    throw new BusinessException(ErrorCode.PARAMS_ERROR,"修改队伍为加密需要密码");
                }
            }
        }
        Team updatedTeam = new Team();
        BeanUtils.copyProperties(teamUpdateRequest,updatedTeam);
        return this.updateById(updatedTeam);
    }

    @Override
    public boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser) {
        if (teamJoinRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = teamJoinRequest.getTeamId();
        Long userId = loginUser.getId();
        if (teamId == null||teamId<0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"teamId不正确");
        }
        if (userId == null||userId<0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"UserId不正确");
        }
        Team targetTeam = this.getById(teamId);
        if(targetTeam==null){
            throw new BusinessException(ErrorCode.NULL_ERROR,"队伍不存在");
        }
        TeamStatusEnum teamStatusEnum = TeamStatusEnum.getTeamStatusEnumByValue(targetTeam.getStatus());
        if(teamStatusEnum.equals(TeamStatusEnum.SECRET)){
            String password = teamJoinRequest.getPassword();
            if(StringUtils.isBlank(password)){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"加入加密队伍需要密码");
            }
            if(!password.equals(targetTeam.getPassword())){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码不正确");
            }
        }
        if(teamStatusEnum.equals(TeamStatusEnum.PRIVATE)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"禁止加入私有队伍");
        }
        //每人最多加入5个队伍
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("USER_ID",userId);
        if(userTeamService.count(queryWrapper)>5){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"最多加入5个队伍");
        }

        //队伍人数不能超过设置的最大人数
        queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("TEAM_ID",teamId);
        if(userTeamService.count(queryWrapper)>=targetTeam.getMaxNum()){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍已满");
        }

        //不能重复加入已加入的队伍
        queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("TEAM_ID",teamId);
        queryWrapper.eq("USER_ID",userId);
        if(userTeamService.count(queryWrapper)>0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"不能重复加入队伍");
        }

        //修改队伍信息
        UserTeam userTeam = new UserTeam();
        userTeam.setTeamId(teamId);
        userTeam.setUserId(userId);
        userTeam.setJoinTime(new Date());
        return userTeamService.save(userTeam);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser) {
//        1. 校验请求参数
        if (teamQuitRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = teamQuitRequest.getTeamId();
        if(teamId==null||teamId<0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long userId = loginUser.getId();
        if(userId==null||userId<0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
//        2. 校验队伍是否存在
        Team team = this.getById(teamId);
        if(team==null){
            throw new BusinessException(ErrorCode.NULL_ERROR,"要退出的队伍不存在");
        }
//        3. 校验我是否已加入队伍
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("TEAM_ID",teamId);
        queryWrapper.eq("USER_ID",userId);
        if(userTeamService.count(queryWrapper)<1){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户未加入队伍");
        }
//        4. 如果队伍
//        a. 只剩一人，队伍解散
        Long teamHasJoinNum = this.countTeamUserByTeamId(teamId);
        if(teamHasJoinNum<=1){
            this.removeById(teamId);
        } else {
            // b. 还有其他人
            // ⅰ. 如果是队长退出队伍，权限转移给第二早加入的用户 —— 先来后到只用取 id 最小的 2 条数据
            if(userId==team.getUserId()){
                QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
                userTeamQueryWrapper.eq("TEAM_ID",teamId);
                userTeamQueryWrapper.last("order by id asc limit 2");
                List<UserTeam> userTeams = userTeamService.list(userTeamQueryWrapper);
                if (CollectionUtils.isEmpty(userTeams)||userTeams.size()<=1) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR);
                }
                UserTeam userTeam = userTeams.get(1);
                Long newUserId = userTeam.getUserId();
                Team newUpdatedTeam = new Team();
                newUpdatedTeam.setUserId(newUserId);
                newUpdatedTeam.setId(teamId);
                boolean result = this.updateById(newUpdatedTeam);
                if(!result){
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR,"更新队伍队长失败");
                }
            }
        }
        return userTeamService.remove(queryWrapper);
    }


    /**
     * 删除队伍
     * @param id
     * @param loginUser
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTeam(long id, User loginUser) {
        if(id<0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if(loginUser==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long userId = loginUser.getId();
        if(userId==null||userId<0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = this.getById(id);
        if(team==null){
            throw new BusinessException(ErrorCode.NULL_ERROR,"要删除的队伍不存在");
        }
        boolean isAdmin = userService.isAdmin(loginUser);
        Long teamLeaderId = team.getUserId();
        if(!teamLeaderId.equals(userId)&&!isAdmin){
            throw new BusinessException(ErrorCode.NO_AUTH,"不是队长或管理员不能删除队伍");
        }
        //删除用户队伍关系
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("TEAM_ID",id);
        boolean result = userTeamService.remove(userTeamQueryWrapper);
        if(!result){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"删除用户队伍关系失败");
        }
        //删除队伍
        return this.removeById(id);
    }


    /**
     * 获取当前队伍目前已加入队伍的人数
     * @param teamId
     * @return
     */
    private long countTeamUserByTeamId(long teamId){
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("TEAM_ID",teamId);
        return userTeamService.count(queryWrapper);
    }


}










