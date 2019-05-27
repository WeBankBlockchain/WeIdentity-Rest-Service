/*
 *       Copyright© (2019) WeBank Co., Ltd.
 *
 *       This file is part of weidentity-http-service.
 *
 *       weidentity-http-service is free software: you can redistribute it and/or modify
 *       it under the terms of the GNU Lesser General Public License as published by
 *       the Free Software Foundation, either version 3 of the License, or
 *       (at your option) any later version.
 *
 *       weidentity-http-service is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU Lesser General Public License for more details.
 *
 *       You should have received a copy of the GNU Lesser General Public License
 *       along with weidentity-http-service.  If not, see <https://www.gnu.org/licenses/>.
 */

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

import com.webank.weid.http.util.JsonUtil;

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
