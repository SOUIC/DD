package com.atguigu.daijia.order.mapper;

import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {

    Long getStatusById(@Param("orderId") Long orderId);

}
