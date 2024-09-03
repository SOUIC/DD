package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.common.util.AuthContextHolder;
import com.atguigu.daijia.dispatch.client.NewOrderFeignClient;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.driver.service.DriverService;
import com.atguigu.daijia.map.client.LocationFeignClient;
import com.atguigu.daijia.model.entity.driver.DriverInfo;
import com.atguigu.daijia.model.form.driver.DriverFaceModelForm;
import com.atguigu.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.lang.String;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class DriverServiceImpl implements DriverService {

    @Resource
    private DriverInfoFeignClient driverInfoFeignClient;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private LocationFeignClient locationFeignClient;

    @Resource
    private NewOrderFeignClient newOrderFeignClient;


    @Override
    public String login(String code) {
        Result<Long> driverInfo = driverInfoFeignClient.login(code);

        if (driverInfo.getCode() != 200) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        Long driverID = driverInfo.getData();

        // 判断返回司机ID是否为空 如果为空 返回错误提示
        if (driverID == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        // 生成token
        String token = UUID.randomUUID().toString().replaceAll("-", "");

        //k: token v:customerID 设置过期实践
        redisTemplate.opsForValue().set(
                RedisConstant.USER_LOGIN_KEY_PREFIX + token,
                driverID,
                RedisConstant.USER_LOGIN_REFRESH_KEY_TIMEOUT,
                TimeUnit.SECONDS
        );

        return token;
    }

    @Override
    public DriverLoginVo getDriverLoginInfo() {
        Long driverID = AuthContextHolder.getUserId();

        Result<DriverLoginVo> loginVoResult = driverInfoFeignClient.getDriverLoginInfo(driverID);

        return loginVoResult.getData();
    }

    @Override
    public DriverAuthInfoVo getDriverAuthInfo(Long driverId) {
        return driverInfoFeignClient.getDriverAuthInfo(driverId).getData();
    }

    @Override
    public Boolean updateDriverAuthInfo(UpdateDriverAuthInfoForm updateDriverAuthInfoForm) {
        return driverInfoFeignClient.updateDriverAuthInfo(updateDriverAuthInfoForm).getData();
    }

    @Override
    public Boolean createDriverFaceModel(DriverFaceModelForm driverFaceModelForm) {
        return driverInfoFeignClient.createDriverFaceModel(driverFaceModelForm).getData();
    }

    @Override
    public Boolean startService(Long driverId) {
        driverInfoFeignClient.updateServiceStatus(driverId,1);
        locationFeignClient.removeDriverLocation(driverId);
        newOrderFeignClient.clearNewOrderQueueData(driverId);
        return Boolean.TRUE;
    }

    @Override
    public Boolean stopService(Long driverId) {
        //更新司机的接单状态 0
        driverInfoFeignClient.updateServiceStatus(driverId,0);
        //删除司机位置信息
        locationFeignClient.removeDriverLocation(driverId);
        //清空司机临时队列
        newOrderFeignClient.clearNewOrderQueueData(driverId);
        return Boolean.TRUE;
    }

    @Override
    public Boolean isFaceRecognition(Long driverId) {
        return Boolean.TRUE;
    }



}
