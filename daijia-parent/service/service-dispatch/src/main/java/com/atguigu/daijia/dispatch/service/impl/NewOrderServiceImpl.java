package com.atguigu.daijia.dispatch.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.dispatch.mapper.OrderJobMapper;
import com.atguigu.daijia.dispatch.service.NewOrderService;
import com.atguigu.daijia.dispatch.xxl.client.XxlJobClient;
import com.atguigu.daijia.map.client.LocationFeignClient;
import com.atguigu.daijia.model.entity.dispatch.OrderJob;
import com.atguigu.daijia.model.enums.OrderStatus;
import com.atguigu.daijia.model.form.map.SearchNearByDriverForm;
import com.atguigu.daijia.model.vo.dispatch.NewOrderTaskVo;
import com.atguigu.daijia.model.vo.map.NearByDriverVo;
import com.atguigu.daijia.model.vo.order.NewOrderDataVo;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class NewOrderServiceImpl implements NewOrderService {

    @Resource
    private OrderJobMapper orderJobMapper;

    @Resource
    private XxlJobClient xxlJobClient;

    @Resource
    private OrderInfoFeignClient orderInfoFeignClient;

    @Resource
    private LocationFeignClient locationFeignClient;

    @Resource
    private RedisTemplate redisTemplate;

    @Override
    public Long addAndStartTask(NewOrderTaskVo newOrderTaskVo) {

        // 判断当前订单是否启动任务调度
        // 根据订单id查询
        OrderJob orderJob = orderJobMapper.selectById(newOrderTaskVo.getOrderId());

        // 没有启动的话
        if (orderJob == null) {
            Long jobId = xxlJobClient.addAndStart(
                    "newOrderTaskHandler",
                    "",
                    "0 0/1 * * * ?",
                    "新建订单任务调度" + newOrderTaskVo.getOrderId().toString()
            );

            orderJob = new OrderJob();
            orderJob.setOrderId(newOrderTaskVo.getOrderId());
            orderJob.setJobId(jobId);
            orderJob.setParameter(JSONObject.toJSONString(newOrderTaskVo));
            orderJobMapper.insert(orderJob);
        }

        return orderJob.getJobId();
    }

    @Override
    public void executeTask(long jobId) {
        // 根据jobId查询数据库，当前任务是否已经创建
        // 如果没有创建，不往下执行了
        OrderJob orderJob = orderJobMapper.selectById(jobId);

        if (orderJob == null) {
            return ;
        }

       //  查询订单状态，如果当前订单接单状态，继续执行。如果当前订单不是接单状态，停止任务调度
        Integer status = orderInfoFeignClient.getOrderStatus(orderJob.getOrderId()).getData();

        if(status.intValue() != OrderStatus.WAITING_ACCEPT.getStatus().intValue()) {
            //停止任务调度
            xxlJobClient.stopJob(jobId);
            return;
        }

        // 远程调用:搜索附近满足条件可以接单司机
        String jsonString = orderJob.getParameter();
        NewOrderTaskVo newOrderTaskVo = JSONObject.parseObject(jsonString, NewOrderTaskVo.class);

        SearchNearByDriverForm searchNearByDriverForm = new SearchNearByDriverForm();
        searchNearByDriverForm.setLatitude(newOrderTaskVo.getStartPointLatitude());
        searchNearByDriverForm.setLongitude(newOrderTaskVo.getStartPointLongitude());
        searchNearByDriverForm.setMileageDistance(newOrderTaskVo.getExpectDistance());

        // 远程调用之后，获取满足可以接单司机集合
        List<NearByDriverVo> nearByDriverVoList = locationFeignClient.searchNearByDriver(searchNearByDriverForm).getData();

        // 遍历司机集合，得到每个司机，为每个司机创建临时队列，存储新订单信息
        nearByDriverVoList.forEach(driver -> {
            // 使用Redis的set类型
            // 根据订单id生成key
            String repeatKey = RedisConstant.DRIVER_ORDER_REPEAT_LIST+newOrderTaskVo.getOrderId();

            Boolean isMember = redisTemplate.opsForSet().isMember(repeatKey, driver.getDriverId());

            if (Boolean.FALSE.equals(isMember)) {
                redisTemplate.opsForSet().add(repeatKey, driver.getDriverId());

                redisTemplate.expire(
                        repeatKey,
                        RedisConstant.DRIVER_ORDER_REPEAT_LIST_EXPIRES_TIME,
                        TimeUnit.MINUTES
                );

                NewOrderDataVo newOrderDataVo = new NewOrderDataVo();
                newOrderDataVo.setOrderId(newOrderTaskVo.getOrderId());
                newOrderDataVo.setStartLocation(newOrderTaskVo.getStartLocation());
                newOrderDataVo.setEndLocation(newOrderTaskVo.getEndLocation());
                newOrderDataVo.setExpectAmount(newOrderTaskVo.getExpectAmount());
                newOrderDataVo.setExpectDistance(newOrderTaskVo.getExpectDistance());
                newOrderDataVo.setExpectTime(newOrderTaskVo.getExpectTime());
                newOrderDataVo.setFavourFee(newOrderTaskVo.getFavourFee());
                newOrderDataVo.setDistance(driver.getDistance());
                newOrderDataVo.setCreateTime(newOrderTaskVo.getCreateTime());

                //新订单保存司机的临时队列，Redis里面List集合
                String key = RedisConstant.DRIVER_ORDER_TEMP_LIST+driver.getDriverId();
                redisTemplate.opsForList().leftPush(key,JSONObject.toJSONString(newOrderDataVo));
                //过期时间：1分钟
                redisTemplate.expire(key,RedisConstant.DRIVER_ORDER_TEMP_LIST_EXPIRES_TIME, TimeUnit.MINUTES);
            }
        });
    }

    @Override
    public List<NewOrderDataVo> findNewOrderQueueData(Long driverId) {
        List<NewOrderDataVo> list = new ArrayList<>();

        String key = RedisConstant.DRIVER_ORDER_TEMP_LIST + driverId;
        Long size = redisTemplate.opsForList().size(key);

        if (size > 0) {
            for (int i = 0; i < size; ++i) {
                String content = String.valueOf(redisTemplate.opsForList().rightPop(key));
                NewOrderDataVo newOrderDataVo = JSONObject.parseObject(content, NewOrderDataVo.class);
                list.add(newOrderDataVo);
            }
        }

        return list;
    }

    @Override
    public Boolean clearNewOrderQueueData(Long driverId) {
        String key = RedisConstant.DRIVER_ORDER_TEMP_LIST + driverId;
        redisTemplate.delete(key);
        return true;
    }
}
