package com.mai.friendsFinder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mai.friendsFinder.mapper.FriendMapper;
import com.mai.friendsFinder.mapper.ItemMapper;
import com.mai.friendsFinder.mapper.UserMapper;
import com.mai.friendsFinder.model.Friend;
import com.mai.friendsFinder.model.Item;
import com.mai.friendsFinder.model.User;
import com.mai.friendsFinder.model.UserItem;
import com.mai.friendsFinder.model.vo.UserMatchVO;
import com.mai.friendsFinder.service.UserItemService;
import com.mai.friendsFinder.service.UserService;
import com.mai.friendsFinder.exception.BusinessException;
import com.mai.friendsFinder.common.ErrorCode;
import com.mai.friendsFinder.utils.AlgorithmUtils;
import com.mai.friendsFinder.utils.UserRecommendUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
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

    @Resource
    private UserItemService userItemService;

    @Resource
    private ItemMapper itemMapper;

    @Resource
    private FriendMapper friendMapper;

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
            throw new BusinessException(ErrorCode.NOT_LOGIN,"从session获取当前用户信息时id为空");
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

    @Override
    public List<User> matchUsers(long num, User loginUser) {
        if (loginUser == null || loginUser.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "登录用户为空或ID为空");
        }

        try {
            // 1. 从数据库加载所有用户-物品关系数据
            List<UserItem> allUserItems = userItemService.list();
            if (allUserItems == null || allUserItems.isEmpty()) {
                log.warn("用户-物品关系数据为空，无法生成用户推荐");
                // 降级方案：返回空列表
                return new ArrayList<>();
            }

            // 2. 创建用户推荐工具实例，构建图
            UserRecommendUtils userRecommendUtils = new UserRecommendUtils(allUserItems);
            log.info("用户推荐图构建完成，节点数: {}, 边数: {}",
                    userRecommendUtils.getNodeCount(),
                    userRecommendUtils.getEdgeCount());

            // 3. 执行随机游走算法，获取推荐的相似用户ID列表
            List<Long> recommendedUserIds = userRecommendUtils.getSimilarUsers(loginUser.getId(), (int) num);
            if (recommendedUserIds == null || recommendedUserIds.isEmpty()) {
                log.info("为用户 {} 未生成相似用户推荐", loginUser.getId());
                return new ArrayList<>();
            }

            // 3.1 获取用户的所有好友ID列表（双向查询）
            QueryWrapper<Friend> friendQueryWrapper = new QueryWrapper<>();
            friendQueryWrapper.eq("user_id", loginUser.getId()).or().eq("friend_id", loginUser.getId());
            friendQueryWrapper.select("user_id", "friend_id");
            List<Friend> friendList = friendMapper.selectList(friendQueryWrapper);

            Set<Long> friendIds = new HashSet<>();
            for (Friend friend : friendList) {
                // 添加好友ID（排除自己）
                if (!friend.getUserId().equals(loginUser.getId())) {
                    friendIds.add(friend.getUserId());
                }
                if (!friend.getFriendId().equals(loginUser.getId())) {
                    friendIds.add(friend.getFriendId());
                }
            }
            log.info("用户 {} 已有 {} 个好友，将从推荐中排除", loginUser.getId(), friendIds.size());

            // 3.2 过滤掉已经是好友的用户
            recommendedUserIds = recommendedUserIds.stream()
                    .filter(userId -> !friendIds.contains(userId))
                    .collect(Collectors.toList());

            if (recommendedUserIds.isEmpty()) {
                log.info("过滤好友后，用户 {} 无可推荐的用户", loginUser.getId());
                return new ArrayList<>();
            }

            // 4. 根据用户ID列表查询用户详情
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.in("id", recommendedUserIds);
            Map<Long, List<User>> userIdUserListMap = this.list(queryWrapper).stream()
                    .map(this::getSafetyUser)
                    .collect(Collectors.groupingBy(User::getId));

            // 5. 保证顺序和推荐算法排序一致
            List<User> finalUserList = new ArrayList<>();
            for (Long userId : recommendedUserIds) {
                List<User> users = userIdUserListMap.get(userId);
                if (users != null && !users.isEmpty()) {
                    finalUserList.add(users.get(0));
                }
            }

            log.info("为用户 {} 成功生成 {} 个相似用户推荐", loginUser.getId(), finalUserList.size());
            return finalUserList;

        } catch (BusinessException e) {
            // 业务异常直接抛出
            throw e;
        } catch (Exception e) {
            log.error("为用户 {} 生成相似用户推荐时发生异常", loginUser.getId(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成用户推荐失败：" + e.getMessage());
        }
    }

    /**
     * 用户匹配（包含共同物品信息）
     *
     * @param num 匹配数量
     * @param loginUser 当前登录用户
     * @return 用户匹配结果列表（包含共同物品）
     */
    public List<UserMatchVO> matchUsersWithCommonItems(long num, User loginUser) {
        if (loginUser == null || loginUser.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "登录用户为空或ID为空");
        }

        // Redis 缓存 key
        String redisKey = "mai:match:users:" + loginUser.getId();

        try {
            // 1. 优先从 Redis 缓存获取用户匹配结果
            Object cachedResult = redisTemplate.opsForValue().get(redisKey);
            if (cachedResult != null && cachedResult instanceof List) {
                @SuppressWarnings("unchecked")
                List<UserMatchVO> cachedMatchList = (List<UserMatchVO>) cachedResult;
                log.info("从缓存获取用户 {} 的匹配结果，共 {} 个用户", loginUser.getId(), cachedMatchList.size());

                // 1.1 获取用户的所有好友ID列表（缓存期间可能新增好友）
                QueryWrapper<Friend> friendQueryWrapper = new QueryWrapper<>();
                friendQueryWrapper.eq("user_id", loginUser.getId()).or().eq("friend_id", loginUser.getId());
                friendQueryWrapper.select("user_id", "friend_id");
                List<Friend> friendList = friendMapper.selectList(friendQueryWrapper);

                Set<Long> friendIds = new HashSet<>();
                for (Friend friend : friendList) {
                    if (!friend.getUserId().equals(loginUser.getId())) {
                        friendIds.add(friend.getUserId());
                    }
                    if (!friend.getFriendId().equals(loginUser.getId())) {
                        friendIds.add(friend.getFriendId());
                    }
                }

                // 1.2 从缓存结果中过滤掉已是好友的用户
                List<UserMatchVO> filteredMatchList = cachedMatchList.stream()
                        .filter(matchVO -> !friendIds.contains(matchVO.getUser().getId()))
                        .collect(Collectors.toList());

                log.info("缓存命中后过滤，用户 {} 过滤前 {} 个，过滤后 {} 个匹配用户",
                         loginUser.getId(), cachedMatchList.size(), filteredMatchList.size());

                // 如果过滤后的数量大于等于请求的num，截取返回
                if (filteredMatchList.size() >= num) {
                    return filteredMatchList.subList(0, (int) num);
                }
                // 否则返回全部过滤后的结果
                return filteredMatchList;
            }

            log.info("缓存未命中，开始实时计算用户 {} 的匹配结果", loginUser.getId());

            // 2. 缓存未命中，实时计算用户匹配结果
            // 2.1 从数据库加载所有用户-物品关系数据
            List<UserItem> allUserItems = userItemService.list();
            if (allUserItems == null || allUserItems.isEmpty()) {
                log.warn("用户-物品关系数据为空，无法生成用户推荐");
                return new ArrayList<>();
            }

            // 2.2 创建用户推荐工具实例，构建图
            UserRecommendUtils userRecommendUtils = new UserRecommendUtils(allUserItems);
            log.info("用户推荐图构建完成，节点数: {}, 边数: {}",
                    userRecommendUtils.getNodeCount(),
                    userRecommendUtils.getEdgeCount());

            // 2.3 执行随机游走算法，获取推荐的相似用户ID列表（计算更多结果用于缓存）
            List<Long> recommendedUserIds = userRecommendUtils.getSimilarUsers(loginUser.getId(), 100);
            if (recommendedUserIds == null || recommendedUserIds.isEmpty()) {
                log.info("为用户 {} 未生成相似用户推荐", loginUser.getId());
                return new ArrayList<>();
            }

            // 2.3.1 获取用户的所有好友ID列表（双向查询）
            QueryWrapper<Friend> friendQueryWrapper = new QueryWrapper<>();
            friendQueryWrapper.eq("user_id", loginUser.getId()).or().eq("friend_id", loginUser.getId());
            friendQueryWrapper.select("user_id", "friend_id");
            List<Friend> friendList = friendMapper.selectList(friendQueryWrapper);

            Set<Long> friendIds = new HashSet<>();
            for (Friend friend : friendList) {
                // 添加好友ID（排除自己）
                if (!friend.getUserId().equals(loginUser.getId())) {
                    friendIds.add(friend.getUserId());
                }
                if (!friend.getFriendId().equals(loginUser.getId())) {
                    friendIds.add(friend.getFriendId());
                }
            }
            log.info("用户 {} 已有 {} 个好友，将从推荐中排除", loginUser.getId(), friendIds.size());

            // 2.3.2 过滤掉已经是好友的用户
            recommendedUserIds = recommendedUserIds.stream()
                    .filter(userId -> !friendIds.contains(userId))
                    .collect(Collectors.toList());

            if (recommendedUserIds.isEmpty()) {
                log.info("过滤好友后，用户 {} 无可推荐的用户", loginUser.getId());
                return new ArrayList<>();
            }

            // 2.4 根据用户ID列表查询用户详情
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.in("id", recommendedUserIds);
            Map<Long, List<User>> userIdUserListMap = this.list(queryWrapper).stream()
                    .map(this::getSafetyUser)
                    .collect(Collectors.groupingBy(User::getId));

            // 2.5 构建UserMatchVO列表，包含共同物品信息
            List<UserMatchVO> matchVOList = new ArrayList<>();
            for (Long userId : recommendedUserIds) {
                List<User> users = userIdUserListMap.get(userId);
                if (users == null || users.isEmpty()) {
                    continue;
                }

                User matchedUser = users.get(0);
                UserMatchVO matchVO = new UserMatchVO();
                matchVO.setUser(matchedUser);

                // 获取共同喜欢的物品ID（取前5个）
                List<Long> commonItemIds = userRecommendUtils.getCommonItems(loginUser.getId(), userId, 5);

                // 查询共同物品的详细信息（直接使用 Mapper 避免循环依赖）
                if (commonItemIds != null && !commonItemIds.isEmpty()) {
                    List<Item> commonItems = commonItemIds.stream()
                            .map(itemId -> itemMapper.selectById(itemId))
                            .filter(item -> item != null)
                            .collect(Collectors.toList());
                    matchVO.setCommonItems(commonItems);
                } else {
                    matchVO.setCommonItems(new ArrayList<>());
                }

                matchVOList.add(matchVO);
            }

            // 2.6 将计算结果缓存到 Redis（24小时过期）
            if (!matchVOList.isEmpty()) {
                redisTemplate.opsForValue().set(
                    redisKey,
                    matchVOList,
                    24 * 60 * 60 * 1000,
                    TimeUnit.MILLISECONDS
                );
                log.info("已将用户 {} 的匹配结果缓存到 Redis，共 {} 个用户", loginUser.getId(), matchVOList.size());
            }

            // 2.7 返回请求数量的结果
            log.info("为用户 {} 成功生成 {} 个相似用户推荐（含共同物品信息）",
                    loginUser.getId(), matchVOList.size());
            if (matchVOList.size() > num) {
                return matchVOList.subList(0, (int) num);
            }
            return matchVOList;

        } catch (BusinessException e) {
            // 业务异常直接抛出
            throw e;
        } catch (Exception e) {
            log.error("为用户 {} 生成相似用户推荐时发生异常", loginUser.getId(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成用户推荐（含共同物品）失败：" + e.getMessage());
        }
    }

}




