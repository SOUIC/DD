package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.model.form.driver.DriverFaceModelForm;
import com.atguigu.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import org.springframework.web.bind.annotation.RequestBody;

public interface DriverService {


    String login(String code);

    DriverLoginVo getDriverLoginInfo();

    DriverAuthInfoVo getDriverAuthInfo(Long driverId);

    Boolean updateDriverAuthInfo(UpdateDriverAuthInfoForm updateDriverAuthInfoForm);

    Boolean createDriverFaceModel(@RequestBody DriverFaceModelForm driverFaceModelForm);

    Boolean startService(Long driverId);

    Boolean stopService(Long driverId);

    Boolean isFaceRecognition(Long driverId);


}
