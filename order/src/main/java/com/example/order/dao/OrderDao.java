package com.example.order.dao;

import com.example.common.entities.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrderDao {
    // �½�����
    int insert(Order order);

    // ���¶��� ��0�޸�Ϊ1
    int update(@Param("id") Long id, @Param("userId") Long userId, @Param("status") Integer status);
}
