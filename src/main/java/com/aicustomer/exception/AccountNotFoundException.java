package com.aicustomer.exception;

public class AccountNotFoundException extends BusinessException {

    public AccountNotFoundException() {
        super(1001, "账号不存在");
    }
}
