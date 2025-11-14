package com.mai.friendsFinder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mai.friendsFinder.mapper.UserMapper;
import com.mai.friendsFinder.model.User;
import com.mai.friendsFinder.service.UserService;
import com.mai.friendsFinder.exception.BusinessException;
import com.mai.friendsFinder.common.ErrorCode;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.mai.friendsFinder.constant.UserConstant.ADMIN_ROLE;
import static com.mai.friendsFinder.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户服务实现类
 *
 *
 * @author ljm
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{
    @Autowired
    private UserMapper userMapper;
    /**
     *
     * 盐值，混淆密码
     */
    private static final String SALT = "jimai";
    @Autowired
    private Gson gson;
    private List<String> tagsNameList;

    @Resource
    private RedisTemplate<String,Object> redisTemplate;

    /**
     * 用户登录态键
     */

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword,String planetCode) {
        long result = -1;
        if(!StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode)) {
            if(userAccount.length() >= 4) {
                if (userPassword.length() >= 8 && checkPassword.length() >= 8) {  //密码大于等于8位
                    String regEx = "[a-zA-Z0-9_-]+$";  //用户名可以包含字母、数字、下划线(_)和短横线(-)
                    Matcher matcher = Pattern.compile(regEx).matcher(userAccount);
                    if (matcher.find()) {//账户符合命名规定
                        if (userPassword.equals(checkPassword)) {//密码和校验密码相同
                            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
                            queryWrapper.eq("user_account", userAccount);
                            long count = userMapper.selectCount(queryWrapper);//账户不能重复
                            queryWrapper = new QueryWrapper<>();
                            queryWrapper.eq("planet_code", planetCode);
                            long count1 = userMapper.selectCount(queryWrapper);//星球号不能重复
                            if (count <= 0 && count1<=0) {//2.加密
                                String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
                                User user = new User();
                                user.setUserAccount(userAccount);
                                user.setUserPassword(encryptPassword);
                                user.setPlanetCode(planetCode);
                                boolean saveResult = this.save(user);
                                if (saveResult) {
                                    result = user.getId();
                                }
                            }
                            else{
                                throw new BusinessException(ErrorCode.PARAMS_ERROR,"账户名重复或星球号重复");
                            }
                        }
                        else{
                            throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码和检验密码不一致");
                        }
                    }
                    else{
                        throw new BusinessException(ErrorCode.PARAMS_ERROR,"账户名不符合命名规定");
                    }
                }
                else{
                    throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码小于8位");
                }
            }
            else{
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户账号过短");
            }
        }
        else{
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"参数为空");
        }
        return result;
    }

    @Override
    public User doLogin(String userAccount, String userPassword, HttpServletRequest request) {
        User user = null;
        if(!StringUtils.isAnyBlank(userAccount, userPassword)){
            if(userAccount.length() >= 4) {
                if (userPassword.length() >= 8) {  //密码大于等于8位
                    String regEx = "[a-zA-Z0-9_-]+$";  //用户名可以包含字母、数字、下划线(_)和短横线(-)
                    Matcher matcher = Pattern.compile(regEx).matcher(userAccount);
                    if (matcher.find()) {
                        //加密
                        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
                        //查询用户是否存在
                        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("user_account", userAccount);
                        queryWrapper.eq("user_password", encryptPassword);
                        user = userMapper.selectOne(queryWrapper);
                    }
                }
            }

        }
        //用户不存在
        if(user == null){
            log.info("user login fail,userAccount cannot match userPassword");
            return user;  //(todo)修改为自定义异常
        }

        //脱敏
        User safetyUser = getSafetyUser(user);

        //记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, safetyUser.getId());

        return safetyUser;
    }

    @Override
    public int userLogout(HttpServletRequest request) {
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return 1;
    }


    /**
     * 判断用户是否为管理员
     * @param request
     * @return true-是 false-否
     */
    public boolean isAdmin(HttpServletRequest request){

        User user = getLoginUser(request);
        return user!=null&&user.getUserRole()==ADMIN_ROLE;
    }

    @Override
    public boolean isAdmin(User user) {
        return user!=null&&user.getUserRole()==ADMIN_ROLE;
    }


    /**
     * 修改用户信息
     * @param user
     * @param request
     * @return 1-修改成功 0-修改失败
     */
    @Override
    public int userUpdate(User user, HttpServletRequest request) {
        if (user==null||request.getSession().getAttribute(USER_LOGIN_STATE)==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"要修改的用户或请求用户不存在");
        }
        User loginUser = getLoginUser(request);
        //todo 如果用户没有更新任何值则直接返回请求参数错误
        //如果是管理员身份则可以修改所有用户
        //非管理用户可以修改自己
        if(!isAdmin(request)&&loginUser.getId()!=user.getId()){
            throw new BusinessException(ErrorCode.NO_AUTH,"无权限修改用户信息");
        }
        long userId = user.getId();
        User oldUser = userMapper.selectById(userId);
        if(oldUser==null){
            throw new BusinessException(ErrorCode.NULL_ERROR,"要修改的用户不存在");
        }
        return userMapper.updateById(user);
    }

    /**
     * 获取当前登录用户
     * @param request
     * @return 当前登录User
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        if(request.getSession().getAttribute(USER_LOGIN_STATE) == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"从session获取当前用户信息时id为空");
        }
        long id = (long) request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = new User();
        user = userMapper.selectById(id);
        return getSafetyUser(user);
    }

    /**
     * 用户脱敏
     * @return 脱敏后的用户信息
     */
    @Override
    public User getSafetyUser(User user) {
        //用户脱敏
        if(user == null){
            return null;
        }
        User safetyUser = new User();
        safetyUser.setId(user.getId());
        safetyUser.setUsername(user.getUsername());
        safetyUser.setUserAccount(user.getUserAccount());
        safetyUser.setAvatarUrl(user.getAvatarUrl());
        safetyUser.setGender(user.getGender());
        safetyUser.setPhone(user.getPhone());
        safetyUser.setEmail(user.getEmail());
        safetyUser.setUserStatus(user.getUserStatus());
        safetyUser.setCreateTime(user.getCreateTime());
        safetyUser.setUpdateTime(user.getUpdateTime());
        safetyUser.setUserRole(user.getUserRole());
        safetyUser.setPlanetCode(user.getPlanetCode());
        safetyUser.setProfile(user.getProfile());
        safetyUser.setTags(user.getTags());
        return safetyUser;
    }


    /**
     * sql方法根据标签搜索用户
     *
     * @return List<User>
     *
     * @author ljm
     */
    @Deprecated
    private List<User> searchUsersByTagsSQL(List<String> TagsNameList){
        List<User> usersList = new ArrayList<User>();
        if(CollectionUtils.isEmpty(TagsNameList)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"标签搜索参数错误");
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        for(String tagName : TagsNameList){
            queryWrapper.like("tags", tagName);
        }
        usersList = userMapper.selectList(queryWrapper);
        return usersList;
    }

    @Override
    public List<User> searchUsersByTags(List<String> TagsNameList){
        List<User> usersList = new ArrayList<User>();
        if(CollectionUtils.isEmpty(TagsNameList)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"标签搜索参数错误");
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        usersList = userMapper.selectList(queryWrapper);
        List<User> result = usersList.stream().filter(user -> {
            String tagsStr = user.getTags();
            Set<String> tempTagNamesSet = gson.fromJson(tagsStr, new TypeToken<Set<String>>() {}.getType());
            tempTagNamesSet = Optional.ofNullable(tempTagNamesSet).orElse(new HashSet<>());
            for(String tagName : TagsNameList){
                if(!tempTagNamesSet.contains(tagName)){
                    return false;
                }
            }
            return true;
        }).map(this::getSafetyUser).collect(Collectors.toList());
        return result;
    }

    @Override
    public Page<User> usersRecommend(User loginUser, long pageNo, long pageSize) {
        String redisKey = String.format("shayu:user:recommend:%s", loginUser.getId());
        ValueOperations ops = redisTemplate.opsForValue();
        Page<User> userPage = (Page<User>)ops.get(redisKey);
        if (userPage != null) {
            return userPage;
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        userPage = page(new Page<>(pageNo,pageSize),queryWrapper);
        try{
            ops.set(redisKey,userPage,30000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("redis key error",e.getMessage());
        }
        return userPage;
    }
}




