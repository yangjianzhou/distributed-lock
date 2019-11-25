package com.iwill.zookeeper.configutation;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CuratorBean {

    @Bean
    public CuratorFramework curatorFramework() {
        RetryPolicy retryPolicy = new RetryNTimes(3, 1000);
        CuratorFramework client = CuratorFrameworkFactory.newClient("localhost:2181", retryPolicy);
        return client;
    }
}
