package com.atguigu.daijia.driver.mapper;

import com.atguigu.daijia.model.entity.driver.DriverSet;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DriverSetMapper extends BaseMapper<DriverSet> {

    void updateServiceStatus(@Param("status") Integer status, @Param("driverId") Long driverId);
}
