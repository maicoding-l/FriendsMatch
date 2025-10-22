package com.mai.friendsFinder.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * session托管到redis
 */
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds= 3600*24)
public class RedisSessionConfig {
}

