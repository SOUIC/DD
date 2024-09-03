package com.atguigu.daijia.common.login;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.common.util.AuthContextHolder;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Slf4j
@Component
public class SonicLoginAspect {

    @Resource
    private RedisTemplate redisTemplate;

    @Around("execution(* com.atguigu.daijia.*.controller.*.*(..)) && @annotation(sonicLogin)")
    public Object login(ProceedingJoinPoint joinPoint, SonicLogin sonicLogin) throws Throwable {

        // 获取request
        RequestAttributes attribute = RequestContextHolder.getRequestAttributes();
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) attribute;
        HttpServletRequest request = servletRequestAttributes.getRequest();

        // 拿到token
        String token = request.getHeader("token");

        // 如果是空的 跳转登陆
        if (!StringUtils.hasText(token)) {
            throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);
        }

        // 查询redis
        String customerID = String.valueOf(redisTemplate.opsForValue().get(RedisConstant.USER_LOGIN_KEY_PREFIX + token));
        log.info("customerID:{}",customerID);
        // 查询对应id 把id放到ThreadLocal里面
        if (StringUtils.hasText(customerID)) {
            AuthContextHolder.setUserId(Long.parseLong(customerID));
        }

        // 执行业务方法
        return joinPoint.proceed();
    }

}
