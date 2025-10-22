package com.mai.friendsFinder.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mai.friendsFinder.model.Tag;
import com.mai.friendsFinder.model.User;
import com.mai.friendsFinder.model.request.DeleteRequest;
import com.mai.friendsFinder.model.request.UserLoginRequest;
import com.mai.friendsFinder.model.request.UserRegisterRequest;
import com.mai.friendsFinder.service.UserService;
import com.mai.friendsFinder.common.BaseResponse;
import com.mai.friendsFinder.exception.BusinessException;
import com.mai.friendsFinder.common.ErrorCode;
import com.mai.friendsFinder.common.ResultUtils;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import static com.mai.friendsFinder.constant.UserConstant.ADMIN_ROLE;
import static com.mai.friendsFinder.constant.UserConstant.USER_LOGIN_STATE;


/**
 * 用户接口
 *
 *
 * @author ljm
 */
@RestController
@RequestMapping("/user")
@CrossOrigin(origins = {"http://localhost:5173/"}, allowCredentials = "true")
public class UserController {
    @Autowired
    private UserService userService;

    private ErrorCode errorCode;

    @Operation(summary = "注册接口")
    @PostMapping("/register")
    public  BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest){
        if(userRegisterRequest == null ){
            //return null;
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        System.out.println("==> 到这了吗？");
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String planetCode = userRegisterRequest.getPlanetCode();
        if(StringUtils.isBlank(userAccount) || StringUtils.isBlank(userPassword) || StringUtils.isBlank(checkPassword) || StringUtils.isBlank(planetCode)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long result =  userService.userRegister(userAccount,userPassword,checkPassword,planetCode) ;
        return ResultUtils.success(result);
    }
    @Operation(summary = "登录接口")
    @PostMapping("/login")
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request){
        if(userLoginRequest == null ){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if(StringUtils.isBlank(userAccount) || StringUtils.isBlank(userPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user =  userService.doLogin(userAccount,userPassword,request) ;
        return ResultUtils.success(user);
    }
    @Operation(summary = "注销接口")
    @PostMapping("/logout")
    public BaseResponse<Integer> userLogout(HttpServletRequest request){
        if(request == null ){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Integer i =  userService.userLogout(request);
        return ResultUtils.success(i);
    }
    @Operation(summary = "获取当前用户")
    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request){
        Object userObject = request.getSession().getAttribute(USER_LOGIN_STATE);
        Long userId = (Long) userObject;
        if(userId == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        //TODO 校验用户是否合法
        User user = userService.getById(userId);
        User safetyUser = userService.getSafetyUser(user);
        return ResultUtils.success(safetyUser);
    }

    /**
     * 搜索用户
     * @param username 用户名
     * @param request  http请求用于鉴定是否为管理员
     * @return 搜索出的所有用户
     */
    @Operation(summary = "搜索用户")
    @GetMapping("/search")
    public BaseResponse<List<User>> searchUser(String username,HttpServletRequest request){
        //鉴权
        if(!userService.isAdmin(request)){
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if(StringUtils.isNotBlank(username)){
            queryWrapper.like("username",username);

        }
        List<User> userList = userService.list(queryWrapper);
        List<User> safeList = userList.stream().map(user -> {
            return userService.getSafetyUser(user);
        }).collect(Collectors.toList());
        return ResultUtils.success(safeList);
    }

    /**
     * 用户推荐
     *
     */
    @Operation(summary = "用户推荐")
    @GetMapping("/recommend")
    public BaseResponse<Page<User>> recommendUsers(HttpServletRequest request,
                                                   @RequestParam(name = "pageNo",   required = false, defaultValue = "1")long pageNo,
                                                   @RequestParam(name = "pageSize", required = false, defaultValue = "10")long pageSize){
        User loginUser = userService.getLoginUser(request);
        if(loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        Page<User> userPage = (Page<User>) userService.usersRecommend(loginUser,pageNo,pageSize);
        return ResultUtils.success(userPage);
    }

    /**
     * 通过标签查询用户
     *
     */
    @Operation(summary = "通过标签查询用户")
    @GetMapping("/search/tags")
    public BaseResponse<List<User>> searchUsersbyTags(@RequestParam(required = false) List<String> tagsNameList){
        if(tagsNameList == null || tagsNameList.isEmpty()){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"输入标签为空");
        }
        List<User> userList = userService.searchUsersByTags(tagsNameList);
        return  ResultUtils.success(userList);

    }
    /**
     * 修改用户信息
     * @param user 需要修改的用户
     * @param request 查询发送修改请求的用户，用于鉴权 为本人修改或管理员修改
     *
     */
    @Operation(summary = "修改用户信息")
    @PostMapping("/update")
    public BaseResponse<Integer> updateUser (@RequestBody User user, HttpServletRequest request){
        if (user==null||request.getSession().getAttribute(USER_LOGIN_STATE)==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"要修改的用户或请求用户不存在");
        }
        int result = userService.userUpdate(user,request);
        return ResultUtils.success(result);
    }

    /**
     * 删除用户
     * @param deleteRequest 用户删除请求
     * @param request http请求用于鉴定是否为管理员
     * @return  删除是否成功
     */
    @Operation(summary = "删除用户")
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request){
        long id = deleteRequest.getId();
        System.out.println("isAdmin = " + userService.isAdmin(request));
        if(!userService.isAdmin(request)){
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        if(id <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result =  userService.removeById(id);
        return ResultUtils.success(result);
    }


}
