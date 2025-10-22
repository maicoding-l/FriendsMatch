package com.mai.friendsFinder;

import com.mai.friendsFinder.model.User;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.RequestBody;

/*
Redis测试
@author ljm
 */
@SpringBootTest
public class RedisTest {
    @Resource
    private RedisTemplate redisTemplate;
    @Test
    void testRedis() {
        ValueOperations ops = redisTemplate.opsForValue();
        ops.set("shayuString", "fish");
        ops.set("shayuInt", 1);
        ops.set("shayuDouble", 2.5);
        User user = new User();
        user.setUsername("shayu");
        ops.set("shayuUser", user);
        Object shayu = ops.get("shayuString");
        Assertions.assertTrue("fish".equals((String)shayu));
        Object shayuInt = ops.get("shayuInt");
        Assertions.assertTrue(1==(Integer)shayuInt);
        Object shayuDouble = ops.get("shayuDouble");
        Assertions.assertTrue(2.5==(Double)shayuDouble);
        System.out.println(ops.get("shayuUser"));


    }
}
