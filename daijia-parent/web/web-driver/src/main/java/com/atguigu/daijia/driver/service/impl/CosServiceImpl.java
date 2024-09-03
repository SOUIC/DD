package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.driver.client.CosFeignClient;
import com.atguigu.daijia.driver.service.CosService;
import com.atguigu.daijia.model.vo.driver.CosUploadVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class CosServiceImpl implements CosService {

    @Resource
    private CosFeignClient cosFeignClient;

    @Override
    public CosUploadVo uploadFile(MultipartFile multipartFile, String path) {
        Result<CosUploadVo> cosUploadVoResult = cosFeignClient.upload(multipartFile, path);
        return cosUploadVoResult.getData();
    }
}
