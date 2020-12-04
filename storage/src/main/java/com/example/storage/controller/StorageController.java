package com.example.storage.controller;

import com.example.common.service.StorageApi;
import com.example.storage.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StorageController implements StorageApi{
    @Autowired
    private StorageService storageService;

    @PostMapping(value = "/storage/decrease")
    @Override
    public void decrease(@RequestParam("productId") Long productId, @RequestParam("count") Integer count){
        storageService.decrease(productId,count);
    }

}
