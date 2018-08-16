package com.iwill.db.controller;

import com.iwill.db.service.LockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping
public class TestController {

    @Autowired
    private LockService lockService;

    @RequestMapping("acquire-lock")
    public String acquireLock() throws Exception{
        boolean locked = lockService.acquire("test", 100000L);
        if (!locked){
            return String.valueOf(locked);
        }
        Thread.sleep(10000);
        lockService.release();
        return String.valueOf(locked);
    }

    @RequestMapping("batch-acquire-lock")
    public String batchAcquireLock() {
        ExecutorService executor = Executors.newFixedThreadPool(20);
        for (int i = 0; i < 100; i++) {
            final int index = i;
            for (int j = 0; j < 20; j++) {
                executor.submit(() -> {
                    boolean locked = lockService.acquire("test_" + index, 10000000L);
                    if (locked) {
                        System.out.println("index: " + index + " ,threadId : " + Thread.currentThread().getId() + " ,locked : " + locked);
                    }
                });
            }
        }
        return "success";
    }
}
