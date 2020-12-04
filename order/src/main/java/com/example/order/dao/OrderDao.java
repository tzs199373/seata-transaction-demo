package com.example.order.dao;

import com.example.common.entities.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrderDao {
    // 新建订单
    int insert(Order order);

    // 更新订单 从0修改为1
    int update(@Param("id") Long id, @Param("userId") Long userId, @Param("status") Integer status);
}
