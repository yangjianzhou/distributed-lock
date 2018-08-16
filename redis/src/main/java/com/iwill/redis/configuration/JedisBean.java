package com.iwill.redis.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.Jedis;

@Configuration
public class JedisBean {

    @Bean
    public Jedis jedis(){
        Jedis jedis = new Jedis("redis://127.0.0.1:6379");
        return jedis ;
    }
}
