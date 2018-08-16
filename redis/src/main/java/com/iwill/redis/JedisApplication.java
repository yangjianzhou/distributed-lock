package com.iwill.redis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.iwill.redis")
public class JedisApplication {

    public static void main(String[] args) {
        SpringApplication.run(JedisApplication.class, args);
    }
}
