package com.atguigu.daijia.customer.config;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import jakarta.annotation.Resource;
import me.chanjar.weixin.common.service.WxService;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class WxConfigOperator {

    @Resource
    private WxConfigProperties wxConfigProperties;

    @Bean
    public WxMaService wxMaService() {
        WxMaDefaultConfigImpl wxMaDefaultConfigImpl = new WxMaDefaultConfigImpl();
        wxMaDefaultConfigImpl.setAppid(wxConfigProperties.getAppId());
        wxMaDefaultConfigImpl.setSecret(wxConfigProperties.getSecret());

        WxMaServiceImpl wxMaService = new WxMaServiceImpl();
        wxMaService.setWxMaConfig(wxMaDefaultConfigImpl);

        return wxMaService;
    }

}
