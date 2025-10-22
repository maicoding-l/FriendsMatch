package com.mai.friendsFinder.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mai.friendsFinder.model.Tag;
import com.mai.friendsFinder.service.TagService;
import com.mai.friendsFinder.mapper.TagMapper;
import org.springframework.stereotype.Service;

/**
* @author ljm
* @description 针对表【tag(标签)】的数据库操作Service实现
* @createDate 2025-09-10 17:22:25
*/
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag>
    implements TagService{

}




