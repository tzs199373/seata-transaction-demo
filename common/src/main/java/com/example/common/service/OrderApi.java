package com.example.common.service;


import com.example.common.entities.Order;

public interface OrderApi {
    // 新建订单
    void create(Order order);
}
