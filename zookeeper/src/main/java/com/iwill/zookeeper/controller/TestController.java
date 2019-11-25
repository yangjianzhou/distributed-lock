package com.iwill.zookeeper.controller;

import com.iwill.zookeeper.service.BusinessService;
import com.iwill.zookeeper.service.CuratorClient;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
public class TestController {

    @Autowired
    private CuratorClient curatorClient;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @RequestMapping(value = "/acquire-lock", method = RequestMethod.GET)
    public String acquireLock() {
        try {
            curatorClient.execute("/lock-path", new BusinessService() {
                @Override
                public void handle() {
                    logger.info("do handle business");
                    try {
                        Thread.sleep(10000L);
                    }catch (Exception exp){

                    }

                }
            });
        } catch (Exception exp) {
            logger.error("handle throw exp", exp);
        }
        return "success";
    }

    @RequestMapping(value = "/batch-acquire-lock", method = RequestMethod.GET)
    public String batchAcquireLock() {
        ExecutorService executorService = Executors.newFixedThreadPool(20);
        for (int index = 0; index < 20; index++) {
            executorService.submit(() -> {
                try {
                    curatorClient.execute("/lock-path", new BusinessService() {
                        @Override
                        public void handle() throws Exception {
                            Thread.sleep(100L);
                            logger.info("do handle business");
                        }
                    });
                } catch (Exception exp) {
                    logger.error("handle throw exp", exp);
                }
            });
        }
        return "success";
    }

}
