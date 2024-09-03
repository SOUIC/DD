package com.atguigu.daijia.rules.service.impl;

import com.atguigu.daijia.model.form.rules.FeeRuleRequest;
import com.atguigu.daijia.model.form.rules.FeeRuleRequestForm;
import com.atguigu.daijia.model.vo.rules.FeeRuleResponse;
import com.atguigu.daijia.model.vo.rules.FeeRuleResponseVo;
import com.atguigu.daijia.rules.service.FeeRuleService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class FeeRuleServiceImpl implements FeeRuleService {

    @Resource
    private KieContainer kieContainer;

    @Override
    public FeeRuleResponseVo calculateOrderFee(FeeRuleRequestForm feeRuleRequestForm) {
        // 封装输入对象
        FeeRuleRequest feeRuleRequest = new FeeRuleRequest();
        feeRuleRequest.setDistance(feeRuleRequestForm.getDistance());
        feeRuleRequest.setStartTime(new DateTime(feeRuleRequestForm.getStartTime()).toString("HH:mm:ss"));
        feeRuleRequest.setWaitMinute(feeRuleRequestForm.getWaitMinute());

        // Drools 使用
        KieSession session = kieContainer.newKieSession();

        FeeRuleResponse feeRuleResponse = new FeeRuleResponse();
        session.setGlobal("feeRuleResponse", feeRuleResponse);
        session.insert(feeRuleRequest);

        session.fireAllRules();
        session.dispose();

        // 封装到FeeRuleResponseVo返回
        FeeRuleResponseVo feeRuleResponseVo = new FeeRuleResponseVo();
        BeanUtils.copyProperties(feeRuleResponse, feeRuleResponseVo);
        return feeRuleResponseVo;
    }
}
