package com.mai.friendsFinder.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mai.friendsFinder.model.User;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.ibatis.annotations.Param;

import java.util.List;


public interface UserService extends IService<User> {
    /**
     * 用户登录态
     */
    /**
     * 用户注册
     *
     * @param userAccount 用户账户
     * @param userPassword 用户密码
     * @param checkPassword 校验密码
     * @return
     */
    long userRegister(String userAccount, String userPassword, String checkPassword,String planetCode);


    /**
     * @param userAccount 用户账户
     *  @param userPassword 用户密码
     * @param request 请求
     * @return  脱敏后的用户值
     */
    User doLogin(@Param("userAccount") String userAccount, @Param("userPassword") String userPassword, HttpServletRequest request) ;

    /**
     * 用户注销
     *
     * @param request 请求对象
     * @return void
     */
    int userLogout(HttpServletRequest request);
    /**
     * 用户脱敏
     * @return 脱敏后的用户信息
     */
    User getSafetyUser(User user);

    /**
     * 判断用户是否为管理员
     * @param request
     * @return true-是 false-否
     */
    public boolean isAdmin(HttpServletRequest request);

    /**
     * 判断用户是否为管理员
     * @param user
     * @return true-是 false-否
     */
    public boolean isAdmin(User user);


    /**
     * 修改用户信息
     * @param user
     * @param request
     * @return 1-修改成功 0-修改失败
     */
    public int userUpdate(User user,HttpServletRequest request);


    /**
     * 获取当前登录用户
     * @param request
     * @return 当前登录User
     */
    public User getLoginUser(HttpServletRequest request);

    /**
     * 用标签搜索用户
     *
     * @param TagsNameList
     * @return 通过标签列表搜索到的用户名单
     */
    public List<User> searchUsersByTags(List<String> TagsNameList);

    public Page<User> usersRecommend(User loginUser,long pageNo, long pageSize);

}
