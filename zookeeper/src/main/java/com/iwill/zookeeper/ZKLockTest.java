package com.iwill.zookeeper;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.RetryNTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class ZKLockTest {

    public static final Logger logger = LoggerFactory.getLogger(ZKLockTest.class);

    public static void main(String[] args) {

        RetryPolicy retryPolicy = new RetryNTimes(3, 100);
        CuratorFramework curatorFramework = CuratorFrameworkFactory.newClient("localhost:2181", retryPolicy);

        curatorFramework.start();

        String fullLockPath = String.format("/%s", "test");
        logger.info(String.format("start to acquire zookeeper lock ,fullLockPath = %s", fullLockPath));
        boolean lockAcquired = false;
        InterProcessMutex interProcessMutex = new InterProcessMutex(curatorFramework, fullLockPath);
        try {
            lockAcquired = interProcessMutex.acquire(200, TimeUnit.MILLISECONDS);
        } catch (Exception exp) {
            logger.error(String.format("acquire lock throw exp ,fullLockPath = %s", fullLockPath), exp);
        }
        if (lockAcquired) {
            logger.info(String.format("acquire lock success,fullLockPath = %s", fullLockPath));
        }

        if (interProcessMutex.isOwnedByCurrentThread()) {
            try {
                interProcessMutex.release();
                logger.info(String.format("release lock success ,fullLockPath = %s", fullLockPath));
            } catch (Exception exp) {
                logger.error(String.format("release lock throw exp ,fullLockPath = %s", fullLockPath));
            }

        }

    }
}
