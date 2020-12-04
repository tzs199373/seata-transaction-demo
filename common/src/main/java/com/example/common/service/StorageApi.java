package com.example.common.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "seata-storage-service")
public interface StorageApi {
    @PostMapping(value = "/storage/decrease")
    void decrease(@RequestParam("productId") Long productId, @RequestParam("count") Integer count);
}
