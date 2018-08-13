package com.iwill.redis;

import org.redisson.Redisson;
import org.redisson.RedissonRedLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RedissonLock {

    public static void main(String[] args) {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        final RedissonClient client = Redisson.create(config);
        ExecutorService executorService = Executors.newFixedThreadPool(20);
        for (int lockIndex = 0; lockIndex < 20; lockIndex++) {
            executorService.submit(new Runnable() {
                public void run() {
                    try {
                        RLock lock = client.getLock("test_lock");
                        RedissonRedLock redissonRedLock = new RedissonRedLock(lock);
                        redissonRedLock.lock();
                        boolean locked = lock.isLocked();
                        System.out.println("thread id : " + (Thread.currentThread().getId()) + " , locked :" + locked);
                        boolean lockSuccess = lock.tryLock(3, 10, TimeUnit.SECONDS);
                        System.out.println("thread id : " + (Thread.currentThread().getId()) + " , lockSuccess :" + lockSuccess);
                    } catch (Exception exp) {
                        exp.printStackTrace();
                    }
                }
            });
        }
    }

}
