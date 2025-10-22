package com.mai.friendsFinder.service;

import com.mai.friendsFinder.model.User;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class UserServiceTest {

    @Autowired
    private UserService userService;


    @Test
    void searchUsersByTags() {
        List<String> tagNamesList = Arrays.asList("Java", "Python");
        List<User> userList = userService.searchUsersByTags(tagNamesList);
        for (User user : userList) {
            System.out.println(user);
        }
        Assert.assertNotNull(userList);
    }
}