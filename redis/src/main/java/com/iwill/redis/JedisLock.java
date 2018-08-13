package com.iwill.redis;

import redis.clients.jedis.Jedis;

public class JedisLock {

    public static void main(String[] args) throws Exception{
        Jedis jedis = new Jedis("redis://127.0.0.1:6379");
        jedis.setex("name",10,"yangjianzhou");
    }
}
