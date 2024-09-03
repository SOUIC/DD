package com.atguigu.daijia.dispatch.xxl.job;

import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.stereotype.Component;

@Component
public class DispatchJobHandler {

    @XxlJob("testHandler")
    public void testJobHandler() {
        System.out.println("xxl-job 项目集成测试");
    }

}
