package com.example.storage.service;

import com.example.storage.dao.StorageDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings("all")
public class StorageService {
    @Autowired
    private StorageDao storageDao;

    public void decrease(Long productId, Integer count){
        storageDao.decrease(productId,count);
    }

}
