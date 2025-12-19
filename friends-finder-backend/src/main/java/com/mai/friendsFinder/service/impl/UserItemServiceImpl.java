package com.mai.friendsFinder.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mai.friendsFinder.model.UserItem;
import com.mai.friendsFinder.service.UserItemService;
import com.mai.friendsFinder.mapper.UserItemMapper;
import org.springframework.stereotype.Service;

/**
* @author ljm
* @description 针对表【user_item(用户-物品关系)】的数据库操作Service实现
* @createDate 2025-12-17 10:12:33
*/
@Service
public class UserItemServiceImpl extends ServiceImpl<UserItemMapper, UserItem>
    implements UserItemService{

}




