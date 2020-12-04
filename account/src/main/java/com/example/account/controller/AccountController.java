package com.example.account.controller;

import com.example.account.service.AccountService;
import com.example.common.service.AccountApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
public class AccountController implements AccountApi {
    @Autowired
    private AccountService accountService;
    @PostMapping(value = "/account/decrease")
    public void decrease(Long userId, BigDecimal money) {
        accountService.decrease(userId, money);
    }
}
