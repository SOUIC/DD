package com.atguigu.daijia.map.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.map.service.MapService;
import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class MapServiceImpl implements MapService {

    @Value("${tencent.map.key}")
    private String key;

    @Resource
    private RestTemplate restTemplate;

    @Override
    public DrivingLineVo calculateDrivingLine(CalculateDrivingLineForm calculateDrivingLineForm) {
        String url = "https://apis.map.qq.com/ws/direction/v1/driving/?from={from}&to={to}&key={key}";

        Map<String, String> map = new HashMap<>();
        map.put(
                "from",
                calculateDrivingLineForm.getStartPointLatitude()
                        + ","
                        + calculateDrivingLineForm.getStartPointLongitude()
        );
        map.put(
                "to",
                calculateDrivingLineForm.getEndPointLatitude()
                        + ","
                        + calculateDrivingLineForm.getEndPointLongitude()
        );
        map.put("key", key);

        log.info("map:{}", map);

        JSONObject result = restTemplate.getForObject(url, JSONObject.class, map);

        int status = result.getIntValue("status");

        if (status != 0) {
            log.info("status:{}", status);
            throw new GuiguException(ResultCodeEnum.MAP_FAIL);
        }

        JSONObject route = result.getJSONObject("result").getJSONArray("routes").getJSONObject(0);

        log.info("route:{}", route);

        DrivingLineVo drivingLineVo = new DrivingLineVo();
        drivingLineVo.setDuration(route.getBigDecimal("duration"));

        drivingLineVo.setDistance(
                route.getBigDecimal("distance")
                        .divideToIntegralValue(new BigDecimal(1000))
                        .setScale(2, RoundingMode.HALF_UP)
        );
        drivingLineVo.setPolyline(route.getJSONArray("polyline"));

        return drivingLineVo;
    }
}
