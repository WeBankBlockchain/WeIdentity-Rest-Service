package com.webank.weid.http.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public abstract class BaseService {

    private static final Logger logger = LoggerFactory.getLogger(BaseService.class);

    /**
     * spring context.
     */
    protected static final ApplicationContext context;

    static {
        // initializing spring containers
        context = new ClassPathXmlApplicationContext(new String[]{
            "classpath:SpringApplicationContext.xml"});
        logger.info("initializing spring containers finish...");

    }
}
