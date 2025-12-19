package com.mai.friendsFinder.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mai.friendsFinder.model.Item;
import com.mai.friendsFinder.service.ItemService;
import com.mai.friendsFinder.mapper.ItemMapper;
import org.springframework.stereotype.Service;

/**
* @author ljm
* @description 针对表【item(物品表)】的数据库操作Service实现
* @createDate 2025-12-17 10:12:10
*/
@Service
public class ItemServiceImpl extends ServiceImpl<ItemMapper, Item>
    implements ItemService{

}




