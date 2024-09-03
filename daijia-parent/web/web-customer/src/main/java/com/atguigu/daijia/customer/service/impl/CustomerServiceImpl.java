package com.atguigu.daijia.customer.service.impl;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.customer.client.CustomerInfoFeignClient;
import com.atguigu.daijia.customer.service.CustomerService;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class CustomerServiceImpl implements CustomerService {

    @Resource
    private CustomerInfoFeignClient customerInfoFeignClient;

    @Resource
    private RedisTemplate redisTemplate;
    @Override
    public String login(String code) {
        log.info("login process start code:{}", code);
        // 拿code进行远程调用 返回用户ID
        Result<String> loginResult = customerInfoFeignClient.login(code);
        Integer ansCode = loginResult.getCode();


        // 如果返回失败了 返回错误提示
        if (ansCode != 200) {
            log.error("error code:{}", ansCode);
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        // 获取远程调用返回用户ID
        String customerID = loginResult.getData();
        log.info("loginResult:{}", loginResult.toString());

        // 判断返回用户ID是否为空 如果为空 返回错误提示
        if (customerID == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        // 生成token
        String token = UUID.randomUUID().toString().replaceAll("-", "");

        //k: token v:customerID 设置过期实践
        redisTemplate.opsForValue().set(
                RedisConstant.USER_LOGIN_KEY_PREFIX + token,
                customerID,
                RedisConstant.USER_LOGIN_REFRESH_KEY_TIMEOUT,
                TimeUnit.SECONDS
        );

        return token;
    }

    @Override
    public CustomerLoginVo getCustomerLoginInfo(Long customerID) {


        Result<CustomerLoginVo> customerLoginInfo = customerInfoFeignClient.getCustomerLoginInfo(customerID);

        Integer code = customerLoginInfo.getCode();
        if (code != 200) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        CustomerLoginVo data = customerLoginInfo.getData();
        if (data == null) {
            throw  new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        return data;
    }
}
