package com.aicustomer.exception;

/**
 * 账号不存在：登录时账号在数据库中查不到。
 */
public class AccountNotFoundException extends BusinessException {
    public AccountNotFoundException() {
        super(1002, "账号不存在");
    }
}
