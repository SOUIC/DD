package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.driver.config.TencentCloudProperties;
import com.atguigu.daijia.driver.service.CosService;
import com.atguigu.daijia.model.vo.driver.CosUploadVo;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;

import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.*;
import com.qcloud.cos.region.Region;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class CosServiceImpl implements CosService {

    @Resource
    private TencentCloudProperties tencentCloudProperties;

    @Override
    public String getImageUrl(String path) {
        if (!StringUtils.hasText(path)) return "";

        COSClient cosClient = getCosClient();

        GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(
                tencentCloudProperties.getBucketPrivate(),
                path,
                HttpMethodName.GET
        );

        Date expiration = new DateTime().plusMinutes(15).toDate();
        generatePresignedUrlRequest.setExpiration(expiration);
        URL url = cosClient.generatePresignedUrl(generatePresignedUrlRequest);
        cosClient.shutdown();

        return url.toString();
    }

    @Override
    public CosUploadVo upload(MultipartFile file, String path) {
        COSClient cosClient = getCosClient();

        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(file.getSize());
        objectMetadata.setContentEncoding("UTF-8");
        objectMetadata.setContentType(file.getContentType());

        String fileType = Objects.requireNonNull(file.getOriginalFilename()).substring(
                file.getOriginalFilename().lastIndexOf(".")
        );
        String uploadPath = "/driver/" + path + UUID.randomUUID()
                .toString().replaceAll("-", "") + fileType;

        PutObjectRequest putObjectRequest;

        try {
            putObjectRequest = new PutObjectRequest(tencentCloudProperties.getBucketPrivate(),
                    uploadPath,
                    file.getInputStream(),
                    objectMetadata);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        putObjectRequest.setStorageClass(StorageClass.Standard);
        PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);
        cosClient.shutdown();

        CosUploadVo cosUploadVo = new CosUploadVo();
        cosUploadVo.setUrl(uploadPath);
        cosUploadVo.setShowUrl(getImageUrl(uploadPath));

        return cosUploadVo;
    }

    private COSClient getCosClient() {
        // 1 初始化用户身份信息（secretId, secretKey）。
        // SECRETID 和 SECRETKEY 请登录访问管理控制台 https://console.cloud.tencent.com/cam/capi 进行查看和管理
        String secretId = tencentCloudProperties.getSecretId();//用户的 SecretId，建议使用子账号密钥，授权遵循最小权限指引，降低使用风险。子账号密钥获取可参见 https://cloud.tencent.com/document/product/598/37140
        String secretKey = tencentCloudProperties.getSecretKey();//用户的 SecretKey，建议使用子账号密钥，授权遵循最小权限指引，降低使用风险。子账号密钥获取可参见 https://cloud.tencent.com/document/product/598/37140
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        // 2 设置 bucket 的地域, COS 地域的简称请参见 https://cloud.tencent.com/document/product/436/6224
        // clientConfig 中包含了设置 region, https(默认 http), 超时, 代理等 set 方法, 使用可参见源码或者常见问题 Java SDK 部分。
        Region region = new Region(tencentCloudProperties.getRegion());
        ClientConfig clientConfig = new ClientConfig(region);
        // 这里建议设置使用 https 协议
        // 从 5.6.54 版本开始，默认使用了 https
        clientConfig.setHttpProtocol(HttpProtocol.https);
        // 3 生成 cos 客户端。
        return new COSClient(cred, clientConfig);
    }
}

