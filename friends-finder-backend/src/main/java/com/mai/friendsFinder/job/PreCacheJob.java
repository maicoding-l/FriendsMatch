package com.mai.friendsFinder.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mai.friendsFinder.model.User;
import com.mai.friendsFinder.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author ljm
 *
 * @Description: 数据预热
 */
@Component
@Slf4j
public class PreCacheJob {
    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate<String,Object> redisTemplate;

    @Resource
    private RedissonClient redissonClient;

    //重点用户
    private List<Long> mainUserList = Arrays.asList(1L);

    @Scheduled(cron = "0 15 10 * * ?")
    public void doCacheRecommendUsers() {
        RLock lock = redissonClient.getLock("mai:preCacheJob:doCache:lock");
        try{
            if(lock.tryLock(30000,0,TimeUnit.MICROSECONDS)){
                QueryWrapper<User> queryWrapper = new QueryWrapper<>();
                Page<User> usersPage = userService.page(new Page<>(1, 20), queryWrapper);
                String redisKey = String.format("shayu:user:recommend:%s", mainUserList);
                ValueOperations<String,Object> ops = redisTemplate.opsForValue();
                try{
                    ops.set(redisKey,usersPage,30000, TimeUnit.MILLISECONDS);
                } catch (Exception e){
                    log.error("redis set key error",e);
                }
            }
        }catch (InterruptedException e){
            log.error("doCacheRecommendUsers error",e);
        }finally {
            if(lock.isHeldByCurrentThread()){
                lock.unlock();
            }
        }

    }
}
