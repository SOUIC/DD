package com.atguigu.daijia.map.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.constant.SystemConstant;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.map.service.LocationService;
import com.atguigu.daijia.model.entity.driver.DriverSet;
import com.atguigu.daijia.model.form.map.SearchNearByDriverForm;
import com.atguigu.daijia.model.form.map.UpdateDriverLocationForm;
import com.atguigu.daijia.model.form.map.UpdateOrderLocationForm;
import com.atguigu.daijia.model.vo.map.NearByDriverVo;
import com.atguigu.daijia.model.vo.map.OrderLocationVo;
import io.swagger.v3.oas.models.security.SecurityScheme;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.*;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class LocationServiceImpl implements LocationService {

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private DriverInfoFeignClient driverInfoFeignClient;
    @Override
    public Boolean updateDriverLocation(UpdateDriverLocationForm updateDriverLocationForm) {

        Point point = new Point(
                updateDriverLocationForm.getLongitude().doubleValue(),
                updateDriverLocationForm.getLatitude().doubleValue()
        );

        redisTemplate.opsForGeo().add(
                RedisConstant.DRIVER_GEO_LOCATION,
                point,
                updateDriverLocationForm.getDriverId()
        );

        return true;
    }

    @Override
    public Boolean removeDriverLocation(Long driverId) {
        redisTemplate.opsForGeo().remove(RedisConstant.DRIVER_GEO_LOCATION, driverId.toString());
        return true;
    }

    @Override
    public List<NearByDriverVo> searchNearByDriver(SearchNearByDriverForm searchNearByDriverForm) {
        // 搜索经纬度位置5公里以内的司机
        Point point = new Point(
                searchNearByDriverForm.getLongitude().doubleValue(),
                searchNearByDriverForm.getLatitude().doubleValue()
        );

        // 定义距离，5公里
        Distance distance = new Distance(
                SystemConstant.NEARBY_DRIVER_RADIUS,
                RedisGeoCommands.DistanceUnit.KILOMETERS
        );

        // 创建circle对象，point  distance
        Circle circle = new Circle(point, distance);

        // 定义GEO参数，设置返回结果包含内容
        RedisGeoCommands.GeoRadiusCommandArgs geoRadiusCommandArgs
                = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                .includeDistance().includeCoordinates().sortAscending();

        // 查询 redis 返回集合
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = redisTemplate.opsForGeo().radius(
                RedisConstant.DRIVER_GEO_LOCATION,
                circle,
                geoRadiusCommandArgs
        );

        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> content = results.getContent();

        List<NearByDriverVo> list = new ArrayList<>();

        if (!CollectionUtils.isEmpty(content)) {

            for (GeoResult<RedisGeoCommands.GeoLocation<String>> item : content) {
                log.error("item class:{}",item.getContent().getName() );

                String name = String.valueOf(item.getContent().getName());
                Long driverId = Long.parseLong(name);

                DriverSet driverSet = driverInfoFeignClient.getDriverSet(driverId).getData();



                BigDecimal orderDistance = driverSet.getOrderDistance();

                if (orderDistance.doubleValue() != 0 &&
                        orderDistance.subtract(searchNearByDriverForm
                                .getMileageDistance()).doubleValue() < 0) {
                    continue;
                }

                BigDecimal currentDistance =
                        BigDecimal.valueOf(item.getDistance().getValue()).setScale(2, RoundingMode.HALF_UP);

                BigDecimal acceptDistance = driverSet.getAcceptDistance();
                if(acceptDistance.doubleValue() !=0
                        && acceptDistance.subtract(currentDistance).doubleValue()<0) {
                    continue;
                }

                //封装复合条件数据
                NearByDriverVo nearByDriverVo = new NearByDriverVo();
                nearByDriverVo.setDriverId(driverId);
                nearByDriverVo.setDistance(currentDistance);
                list.add(nearByDriverVo);
            }
        }
        return list;
    }

    @Override
    public Boolean updateOrderLocationToCache(UpdateOrderLocationForm updateOrderLocationForm) {
        OrderLocationVo orderLocationVo = new OrderLocationVo();
        orderLocationVo.setLongitude(updateOrderLocationForm.getLongitude());
        orderLocationVo.setLatitude(updateOrderLocationForm.getLatitude());

        String key = RedisConstant.UPDATE_ORDER_LOCATION + updateOrderLocationForm.getOrderId();
        redisTemplate.opsForValue().set(key,orderLocationVo);
        return true;
    }

    @Override
    public OrderLocationVo getCacheOrderLocation(Long orderId) {
        String key = RedisConstant.UPDATE_ORDER_LOCATION + orderId;
        return (OrderLocationVo)redisTemplate.opsForValue().get(key);
    }
}
