package com.iwill.redis;

import redis.clients.jedis.Jedis;

public class JedisLock {

    public static void main(String[] args) throws Exception{
        Jedis jedis = new Jedis("redis://127.0.0.1:6379");
        long locked = jedis.setnx("name","yangjianzhou");
        System.out.println(locked);
        String script = "return redis.pcall('setnx',KEYS[1],'bar')";
        Object object = jedis.eval(script , 1 ,"bar");
        System.out.println(object);
        jedis.time();
    }
}
