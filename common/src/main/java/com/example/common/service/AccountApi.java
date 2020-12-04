package com.example.common.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@FeignClient(value = "seata-account-service")
public interface AccountApi {

    @PostMapping(value = "/account/decrease")
    void decrease(@RequestParam("userId") Long userId, @RequestParam("money") BigDecimal money);

}
