package com.example.order.controller;

import com.example.common.entities.CommonResult;
import com.example.common.entities.Order;
import com.example.common.service.OrderApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class OrderController {
    @Autowired
    private OrderApi orderService;

    @GetMapping("/order/create")
    public CommonResult create(Order order) {
        orderService.create(order);
        return new CommonResult(1, "success");
    }
}
