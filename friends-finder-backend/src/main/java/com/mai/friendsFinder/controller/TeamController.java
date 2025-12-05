package com.mai.friendsFinder.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mai.friendsFinder.common.BaseResponse;
import com.mai.friendsFinder.common.ErrorCode;
import com.mai.friendsFinder.common.PageRequest;
import com.mai.friendsFinder.common.ResultUtils;
import com.mai.friendsFinder.exception.BusinessException;
import com.mai.friendsFinder.model.Team;
import com.mai.friendsFinder.model.User;
import com.mai.friendsFinder.model.UserTeam;
import com.mai.friendsFinder.model.dto.TeamQuery;
import com.mai.friendsFinder.model.request.TeamAddRequest;
import com.mai.friendsFinder.model.request.TeamJoinRequest;
import com.mai.friendsFinder.model.request.TeamQuitRequest;
import com.mai.friendsFinder.model.request.TeamUpdateRequest;
import com.mai.friendsFinder.model.vo.TeamUserVO;
import com.mai.friendsFinder.service.TeamService;
import com.mai.friendsFinder.service.UserService;
import com.mai.friendsFinder.service.UserTeamService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 队伍接口
 *
 *
 * @author ljm
 */
@RestController
@RequestMapping("/team")
@CrossOrigin(origins = {"http://localhost:5173/"}, allowCredentials = "true")
public class TeamController {
    @Autowired
    private TeamService teamService;
    @Autowired
    private UserService userService;
    @Autowired
    private UserTeamService userTeamService;

    /**
     * 新增队伍
     * @params teamRequest
     *
     * @author ljm
     */
    @PostMapping("/add")
    public BaseResponse<Long> addTeam(@RequestBody TeamAddRequest teamAddRequest, HttpServletRequest request) {
        if(teamAddRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍为空");
        }
        User user = userService.getLoginUser(request);
        if(user==null){
            throw new BusinessException(ErrorCode.NO_AUTH,"未登录");
        }
        Team team = new Team();
        BeanUtils.copyProperties(teamAddRequest, team);
        long result = teamService.addTeam(team, user);
        if(result<0){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"新建队伍失败");
        }
        return ResultUtils.success(result);
    }

    /**
     * 删除队伍
     * @params id
     *
     * @author ljm
     */
    @Operation(summary = "解散队伍")
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTeam(@RequestParam Long id, HttpServletRequest request) {
        if(id==null||id<0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍id不正确");
        }
        User loginUser = userService.getLoginUser(request);
        if(loginUser==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = teamService.deleteTeam(id,loginUser);
        if(!result){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"删除队伍失败");
        }
        return ResultUtils.success(result);
    }

    /**
     * 修改队伍
     * @params team
     *
     * @author ljm
     */
    @Operation(summary = "修改队伍信息")
    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateRequest teamUpdateRequest, HttpServletRequest request) {
        if(teamUpdateRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍参数错误");
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.updateTeam(teamUpdateRequest,loginUser);
        if(!result){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"队伍修改失败");
        }
        return ResultUtils.success(result);
    }

    /**
     * 查询单个队伍
     * @params id
     *
     * @author ljm
     */
    @Operation(summary = "查询单个队伍")
    @GetMapping
    public BaseResponse<Team> getTeam(Long id) {
        if(id<0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"查询队伍参数错误");
        }
        Team team = teamService.getById(id);
        if(team==null){
            throw new BusinessException(ErrorCode.NULL_ERROR,"查询单个队伍失败");
        }
        return ResultUtils.success(team);
    }

    /**
     * 查询多个team
     *
     * @param teamQuery
     * @return
     *
     * @author ljm
     */
    @Operation(summary = "查询多个team")
    @GetMapping("/list")
    public BaseResponse<List<TeamUserVO>> listTeams(TeamQuery teamQuery, HttpServletRequest request) {
        if(teamQuery==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"参数为空");
        }
        boolean isAdmin = userService.isAdmin(request);
        List<TeamUserVO>  teamUserVOList = teamService.listTeams(teamQuery, isAdmin);
        return ResultUtils.success(teamUserVOList);
    }


    /**分页查询队伍
     *
     * @param teamQuery
     * @return
     *
     * @author ljm
     */
    @Operation(summary = "分页查询队伍")
    @GetMapping("/list/page")
    public BaseResponse<Page<Team>> listTeamsByPage(TeamQuery teamQuery){
        if(teamQuery==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"参数为空");
        }
        Team team = new Team();
        BeanUtils.copyProperties(teamQuery,team);
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>(team);
        Page<Team>  teamPage = new Page<>(teamQuery.getPageNo(),teamQuery.getPageSize());
        Page<Team> resultPage = teamService.page(teamPage,queryWrapper);
        return ResultUtils.success(resultPage);
    }

    /**
     * 用户加入队伍
     * @param teamJoinRequest
     * @param request
     * @return
     *
     * @author ljm
     */
    @Operation(summary = "用户加入队伍")
    @PostMapping("/join")
    public BaseResponse<Boolean> joinTeam(@RequestBody TeamJoinRequest teamJoinRequest, HttpServletRequest request) {
        if(teamJoinRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        if(loginUser==null){
            throw new BusinessException(ErrorCode.NO_AUTH,"未登录");
        }
        boolean result = teamService.joinTeam(teamJoinRequest, loginUser);
        return ResultUtils.success(result);
    }


    /**
     * 用户退出队伍
     * @param teamQuitRequest
     * @param request
     * @return
     *
     * @author ljm
     */
    @Operation(summary = "用户退出队伍")
    @PostMapping("/quit")

    public BaseResponse<Boolean> quitTeam(@RequestBody TeamQuitRequest teamQuitRequest, HttpServletRequest request) {
        if(teamQuitRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        if(loginUser==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = teamService.quitTeam(teamQuitRequest, loginUser);
        return ResultUtils.success(result);
    }


    /**
     * 获取我创建的队伍
     * @param teamQuery
     * @param request
     * @return
     *
     * @author ljm
     */
    @Operation(summary = "获取我创建的队伍")
    @GetMapping("list/my/create")
    public BaseResponse<List<TeamUserVO>> listMyCreateTeam(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        if (user == null) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        teamQuery.setUserId(user.getId());
        List<TeamUserVO> teamList = teamService.listTeams(teamQuery,true);
        return ResultUtils.success(teamList);
    }


    /**
     * 获取我加入的队伍
     * @param teamQuery
     * @param request
     * @return
     *
     * @author ljm
     */
    @Operation(summary = "获取我加入的队伍")
    @GetMapping("/list/my/join")
    public BaseResponse<List<TeamUserVO>> listMyJoinTeam(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        if (user == null) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", user.getId());
        List<UserTeam> userTeamList = userTeamService.list(queryWrapper);
        //取出不重复的队伍id
        //teamId userId
        Map<Long,List<UserTeam>> listMap = userTeamList.stream().collect(Collectors.groupingBy(UserTeam::getTeamId));
        List<Long> idList = new ArrayList<>(listMap.keySet());
        teamQuery.setIdList(idList);
        List<TeamUserVO> teamUserVOList = teamService.listTeams(teamQuery,true);
        return ResultUtils.success(teamUserVOList);
    }

}
