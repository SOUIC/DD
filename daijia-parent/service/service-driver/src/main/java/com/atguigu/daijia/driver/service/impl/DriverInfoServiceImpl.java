package com.atguigu.daijia.driver.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.atguigu.daijia.common.constant.SystemConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.driver.mapper.DriverAccountMapper;
import com.atguigu.daijia.driver.mapper.DriverInfoMapper;
import com.atguigu.daijia.driver.mapper.DriverLoginLogMapper;
import com.atguigu.daijia.driver.mapper.DriverSetMapper;
import com.atguigu.daijia.driver.service.CosService;
import com.atguigu.daijia.driver.service.DriverInfoService;
import com.atguigu.daijia.model.entity.driver.DriverAccount;
import com.atguigu.daijia.model.entity.driver.DriverInfo;
import com.atguigu.daijia.model.entity.driver.DriverLoginLog;
import com.atguigu.daijia.model.entity.driver.DriverSet;
import com.atguigu.daijia.model.form.driver.DriverFaceModelForm;
import com.atguigu.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class DriverInfoServiceImpl extends ServiceImpl<DriverInfoMapper, DriverInfo> implements DriverInfoService {

    @Resource
    private WxMaService wxMaService;

    @Resource
    private DriverInfoMapper driverInfoMapper;

    @Resource
    private DriverSetMapper driverSetMapper;

    @Resource
    private DriverAccountMapper driverAccountMapper;

    @Resource
    private DriverLoginLogMapper driverLoginLogMapper;

    @Resource
    private CosService cosService;
    @Override
    public Long login(String code) {
        String openId;
        try {
            WxMaJscode2SessionResult sessionInfo = wxMaService.getUserService().getSessionInfo(code);
            openId = sessionInfo.getOpenid();
        } catch (WxErrorException e) {
            log.error("Failed to get user session info from WeChat Mini Program.", e);
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        DriverInfo driverInfo = findOrCreateDriverInfo(openId);
        recordLoginInfo(driverInfo);

        return driverInfo.getId();
    }

    @Override
    public DriverLoginVo getDriverLoginInfo(Long driverCode) {
        DriverInfo driverInfo = driverInfoMapper.selectById(driverCode);
        DriverLoginVo driverLoginVo = new DriverLoginVo();
        BeanUtils.copyProperties(driverInfo, driverLoginVo);

        String faceModelId = driverInfo.getFaceModelId();
        boolean hasText = StringUtils.hasText(faceModelId);
        driverLoginVo.setIsArchiveFace(hasText);

        return driverLoginVo;
    }

    @Override
    public DriverAuthInfoVo getDriverAuthInfo(Long driverId) {
        DriverInfo driverInfo = driverInfoMapper.selectById(driverId);
        DriverAuthInfoVo driverAuthInfoVo = new DriverAuthInfoVo();
        BeanUtils.copyProperties(driverInfo, driverAuthInfoVo);

        /*
        没东西所以注释掉了
        driverAuthInfoVo.setIdcardBackShowUrl(cosService.getImageUrl(driverAuthInfoVo.getIdcardBackUrl()));
        driverAuthInfoVo.setIdcardFrontShowUrl(cosService.getImageUrl(driverAuthInfoVo.getIdcardFrontUrl()));

        driverAuthInfoVo.setDriverLicenseBackShowUrl(cosService.getImageUrl(driverAuthInfoVo.getDriverLicenseBackUrl()));
        driverAuthInfoVo.setDriverLicenseFrontShowUrl(cosService.getImageUrl(driverAuthInfoVo.getDriverLicenseFrontUrl()));

        driverAuthInfoVo.setIdcardHandShowUrl(cosService.getImageUrl(driverAuthInfoVo.getIdcardHandUrl()));
        driverAuthInfoVo.setDriverLicenseHandShowUrl(cosService.getImageUrl(driverAuthInfoVo.getDriverLicenseHandUrl()));
        */

        return driverAuthInfoVo;
    }

    @Override
    public Boolean updateDriverAuthInfo(UpdateDriverAuthInfoForm driverAuthInfoForm) {
        Long driverId = driverAuthInfoForm.getDriverId();

        DriverInfo driverInfo = new DriverInfo();
        driverInfo.setId(driverId);
        BeanUtils.copyProperties(driverAuthInfoForm, driverInfo);

        return driverInfoMapper.updateById(driverInfo) > 0;
    }

    @Override
    public void createDriverFaceModel(DriverFaceModelForm driverFaceModelForm) {
        Long driverId = driverFaceModelForm.getDriverId();
        DriverInfo driverInfo = driverInfoMapper.selectById(driverId);
        driverInfo.setFaceModelId("1");
        driverInfoMapper.updateById(driverInfo);
    }

    @Override
    public DriverSet getDriverSet(Long driverId) {
        DriverSet driverSet = driverSetMapper.selectById(driverId);
        log.info("driverSet:{}, driverId:{}", driverSet, driverId);

        return driverSetMapper.selectById(driverId);
    }

    @Override
    public Boolean updateServiceStatus(Long driverId, Integer status) {
        // update driver_set set status=? where driver_id=?
        DriverSet driverSet = new DriverSet();
        driverSet.setServiceStatus(status);
        driverSetMapper.updateServiceStatus(status, driverId);
        return true;
    }

    @Override
    public DriverInfoVo getDriverInfoOrder(Long driverId) {
        //司机id获取基本信息
        DriverInfo driverInfo = driverInfoMapper.selectById(driverId);

        //封装DriverInfoVo
        DriverInfoVo driverInfoVo = new DriverInfoVo();
        BeanUtils.copyProperties(driverInfo,driverInfoVo);

        //计算驾龄
        //获取当前年
        int currentYear = new DateTime().getYear();
        //获取驾驶证初次领证日期
        //driver_license_issue_date
        int firstYear = new DateTime(driverInfo.getDriverLicenseIssueDate()).getYear();
        int driverLicenseAge = currentYear - firstYear;
        driverInfoVo.setDriverLicenseAge(driverLicenseAge);

        return driverInfoVo;
    }

    private DriverInfo findOrCreateDriverInfo(String openId) {
        LambdaQueryWrapper<DriverInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DriverInfo::getWxOpenId, openId);
        DriverInfo driverInfo = driverInfoMapper.selectOne(wrapper);

        if(driverInfo == null) {
            // 初始化司机基本信息
            driverInfo = new DriverInfo();
            driverInfo.setNickname(String.valueOf(System.currentTimeMillis()));
            driverInfo.setWxOpenId(openId);
            driverInfo.setAvatarUrl("https://img0.baidu.com/it/u=3920778814,1443331810&fm=253&app=120&size=w931&n=0&f=JPEG&fmt=auto?sec=1724173200&t=1863b4002cced1a0beac5e5f07f87a75");
            driverInfoMapper.insert(driverInfo);

            // 初始化司机设置
            DriverSet driverSet = new DriverSet();
            driverSet.setDriverId(driverInfo.getId());
            driverSet.setOrderDistance(new BigDecimal(0));
            driverSet.setAcceptDistance(new BigDecimal(SystemConstant.ACCEPT_DISTANCE));
            driverSet.setIsAutoAccept(0); // 是1 否0
            driverSetMapper.insert(driverSet);

            // 初始化司机账户信息
            DriverAccount driverAccount = new DriverAccount();
            driverAccount.setDriverId(driverInfo.getId());
            driverAccountMapper.insert(driverAccount);

        }

        log.info("customerInfo:{}",driverInfo);

        return driverInfo;
    }

    private void recordLoginInfo(DriverInfo driverInfo) {
        DriverLoginLog driverLoginLog = new DriverLoginLog();
        driverLoginLog.setDriverId(driverInfo.getId());
        driverLoginLog.setMsg("小程序登陆");
        driverLoginLogMapper.insert(driverLoginLog);
    }
}