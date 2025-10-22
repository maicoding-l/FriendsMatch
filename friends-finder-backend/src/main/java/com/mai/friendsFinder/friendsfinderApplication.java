package com.mai.friendsFinder;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.mai.friendsFinder.mapper")
public class friendsfinderApplication {

    public static void main(String[] args) {
        SpringApplication.run(friendsfinderApplication.class, args);
    }

}
