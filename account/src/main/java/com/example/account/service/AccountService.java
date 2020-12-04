package com.example.account.service;

import com.example.account.dao.AccountDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@SuppressWarnings("all")
public class AccountService {
    @Autowired
    private AccountDao accountDao;
    public void decrease(Long userId, BigDecimal money) {
        if(money.equals(new BigDecimal(50))){
            throw new RuntimeException("moneny=50,´¥·¢Ä£ÄâÒì³£");
        }
        accountDao.decrease(userId, money);
    }
}
