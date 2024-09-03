package com.atguigu.daijia.customer.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.atguigu.daijia.customer.mapper.CustomerInfoMapper;
import com.atguigu.daijia.customer.mapper.CustomerLoginLogMapper;
import com.atguigu.daijia.customer.service.CustomerInfoService;
import com.atguigu.daijia.model.entity.customer.CustomerInfo;
import com.atguigu.daijia.model.entity.customer.CustomerLoginLog;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class CustomerInfoServiceImpl extends ServiceImpl<CustomerInfoMapper, CustomerInfo> implements CustomerInfoService {
    @Resource
    private WxMaService wxMaService;

    @Resource
    private CustomerInfoMapper customerInfoMapper;

    @Resource
    private CustomerLoginLogMapper customerLoginLogMapper;
    @Override
    public Long login(String code) {
        log.info("Starting login process with code: {}", code);
        String openId;
        try {
            WxMaJscode2SessionResult sessionInfo = wxMaService.getUserService().getSessionInfo(code);
            openId = sessionInfo.getOpenid();
        } catch (WxErrorException e) {
            log.error("Failed to get user session info from WeChat Mini Program.", e);
            throw new RuntimeException(e);
        }

        CustomerInfo customerInfo = findOrCreateCustomerInfo(openId);
        recordLoginInfo(customerInfo);

        log.info("Login successful for customer ID: {}", customerInfo.getId());

        return customerInfo.getId();
    }

    @Override
    public CustomerLoginVo getCustomerLoginInfo(Long customerId) {
        // 查询用户信息
        CustomerInfo customerInfo = customerInfoMapper.selectById(customerId);

        // 绑定到Vo
        CustomerLoginVo customerLoginVo = new CustomerLoginVo();
        BeanUtils.copyProperties(customerInfo, customerLoginVo);

        // 判断是否绑定电话号码
        String phone = customerInfo.getPhone();
        boolean hasText = StringUtils.hasText(phone);
        customerLoginVo.setIsBindPhone(hasText);

        return customerLoginVo;
    }

    private CustomerInfo findOrCreateCustomerInfo(String openId) {
        LambdaQueryWrapper<CustomerInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerInfo::getWxOpenId, openId);
        CustomerInfo customerInfo = customerInfoMapper.selectOne(wrapper);

        if(customerInfo == null) {
            customerInfo = new CustomerInfo();
            customerInfo.setNickname(String.valueOf(System.currentTimeMillis()));
            customerInfo.setWxOpenId(openId);
            customerInfo.setAvatarUrl("https://img0.baidu.com/it/u=3920778814,1443331810&fm=253&app=120&size=w931&n=0&f=JPEG&fmt=auto?sec=1724173200&t=1863b4002cced1a0beac5e5f07f87a75");
            customerInfoMapper.insert(customerInfo);
        }

        log.info("customerInfo:{}",customerInfo);

        return customerInfo;
    }

    private void recordLoginInfo(CustomerInfo customerInfo) {
        CustomerLoginLog customerLoginLog = new CustomerLoginLog();
        customerLoginLog.setCustomerId(customerInfo.getId());
        customerLoginLog.setMsg("小程序登陆");
        customerLoginLogMapper.insert(customerLoginLog);
    }
}
