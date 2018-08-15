package com.iwill.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.utils.CloseableUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class CuratorClient implements InitializingBean, DisposableBean {

    @Autowired
    private CuratorFramework client;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public void execute(String lockPath, BusinessService businessService) throws Exception {
        InterProcessMutex lock = new InterProcessMutex(client, lockPath);
        try {
            boolean acquireLockSuccess = lock.acquire(200, TimeUnit.MILLISECONDS);
            if (!acquireLockSuccess) {
                logger.warn("acquire lock fail , thread id : " + Thread.currentThread().getId());
                return;
            }
            logger.info("acquire lock success ,thread id : " + Thread.currentThread().getId());
            businessService.handle();
        } catch (Exception exp) {
            logger.error("execute throw exp", exp);
        } finally {
            if (lock.isOwnedByCurrentThread()) {
                lock.release();
            }
        }
    }

    /**
     * Invoked by a BeanFactory on destruction of a singleton.
     *
     * @throws Exception in case of shutdown errors.
     *                   Exceptions will get logged but not rethrown to allow
     *                   other beans to release their resources too.
     */
    @Override
    public void destroy() throws Exception {
        CloseableUtils.closeQuietly(client);
    }

    /**
     * Invoked by a BeanFactory after it has set all bean properties supplied
     * (and satisfied BeanFactoryAware and ApplicationContextAware).
     * <p>This method allows the bean instance to perform initialization only
     * possible when all bean properties have been set and to throw an
     * exception in the event of misconfiguration.
     *
     * @throws Exception in the event of misconfiguration (such
     *                   as failure to set an essential property) or if initialization fails.
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        client.start();
    }
}
