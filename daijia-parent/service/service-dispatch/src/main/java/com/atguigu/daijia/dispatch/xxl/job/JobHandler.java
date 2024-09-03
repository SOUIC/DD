package com.atguigu.daijia.dispatch.xxl.job;

import com.atguigu.daijia.dispatch.mapper.XxlJobLogMapper;
import com.atguigu.daijia.dispatch.service.NewOrderService;
import com.atguigu.daijia.model.entity.dispatch.XxlJobLog;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JobHandler {

    @Resource
    private NewOrderService newOrderService;

    @Resource
    private XxlJobLogMapper xxlJobLogMapper;

    @XxlJob("newOrderTaskHandler")
    public void newOrderTaskHandler() {

        XxlJobLog xxlJobLog = new XxlJobLog();
        xxlJobLog.setJobId(XxlJobHelper.getJobId());
        Long startTime = System.currentTimeMillis();

        try {
            newOrderService.executeTask(XxlJobHelper.getJobId());
            xxlJobLog.setStatus(1);
        } catch (Exception e) {
            xxlJobLog.setStatus(0);
            xxlJobLog.setError(e.getMessage());
            log.error(e.getMessage());
        } finally {
            xxlJobLog.setTimes(System.currentTimeMillis() - startTime);
            xxlJobLogMapper.insert(xxlJobLog);
        }

    }

}
