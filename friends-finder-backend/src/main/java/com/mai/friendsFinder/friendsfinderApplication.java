package com.mai.friendsFinder;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.mai.friendsFinder.mapper")
@EnableScheduling
public class friendsfinderApplication {

    public static void main(String[] args) {
        SpringApplication.run(friendsfinderApplication.class, args);
    }

}
