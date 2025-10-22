package com.mai.friendsFinder;
import com.mai.friendsFinder.mapper.UserMapper;
import com.mai.friendsFinder.model.User;
import com.mai.friendsFinder.service.UserService;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class test {
    @Autowired
    private UserMapper userMapper;
    @Test
    public void test() {
        userMapper.delete(null);
        User user = new User();
        userMapper.insert(user);
        assertThat(user.getId()).isNotNull();
        List<User> users = userMapper.selectList(null);
        assertThat(users.size()).isEqualTo(1);
        for (User user1 : users) {
            System.out.println(user1);
        }
    }


}
