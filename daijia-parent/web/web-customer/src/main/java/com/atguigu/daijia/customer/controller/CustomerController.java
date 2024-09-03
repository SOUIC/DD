package com.atguigu.daijia.customer.controller;

import com.atguigu.daijia.common.login.SonicLogin;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.util.AuthContextHolder;
import com.atguigu.daijia.customer.service.CustomerService;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "客户API接口管理")
@RestController
@RequestMapping("/customer")
@SuppressWarnings({"unchecked", "rawtypes"})
public class CustomerController {

    @Resource
    private CustomerService customerService;

    @Operation(summary = "获取客户登陆信息")
    @SonicLogin
    @GetMapping("/getCustomerLoginInfo")
    public Result<CustomerLoginVo> getCustomerLoginInfo() {
        // @RequestHeader(value = "token") String token
        // 另外一种获取token的方式等同于上述方法
        // HttpServletRequest request
        // request.getHeader("token");
        //CustomerLoginVo customerLoginInfo = customerService.getCustomerLoginInfo(token);

        Long customerID = AuthContextHolder.getUserId();

        CustomerLoginVo customerLoginInfo = customerService.getCustomerLoginInfo(customerID);


        return Result.ok(customerLoginInfo);
    }

    @Operation(summary = "小程序登陆")
    @GetMapping("/login/{code}")
    public Result<String> login(@PathVariable String code) {
        return Result.ok(customerService.login(code));
    }



}

