package com.webank.weid.http.aspect;

import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.webank.weid.util.JsonUtil;

@Aspect
@Component
public class ServiceLogAspect {

    private Logger logger = LoggerFactory.getLogger(ServiceLogAspect.class);

    ThreadLocal<Long>  startTime = new ThreadLocal<Long>();

    @Pointcut("execution(public * com.webank.weid.http.service.*.*(..))")
    private void controllerAspect(){

    }

    /**
     * running before joinPoint
     * @param joinPoint this is joinPoint
     */
    @Before(value = "controllerAspect()")
    public void before(JoinPoint joinPoint){

        startTime.set(System.currentTimeMillis());

        ServletRequestAttributes requestAttributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = requestAttributes.getRequest();

        logger.info("####request url:{}| type:{}| method:[{}]| args:{}",
            request.getRequestURL().toString(),
            request.getMethod(),
            joinPoint.getSignature(),
            Arrays.toString(joinPoint.getArgs()));
    }

    /**
     * when joinPoint, running after return
     * @param joinPoint this is joinPoint
     * @param keys return result
     */
    @AfterReturning(value = "controllerAspect()", returning = "keys")
    public void afterReturning(JoinPoint joinPoint, Object keys) {

        logger.info("####response method:[{}]| args:{}| result:{}| cost tims:{}ms",
            joinPoint.getSignature(),
            Arrays.asList(joinPoint.getArgs()),
            JsonUtil.objToJsonStr(keys),
            (System.currentTimeMillis() - startTime.get()));
    }

}
