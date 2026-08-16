package com.aicustomer.exception;

/**
 * 账号已存在：注册时账号在数据库中已有记录。
 */
public class AccountAlreadyExistsException extends BusinessException {
    public AccountAlreadyExistsException() {
        super(1001, "账号已存在");
    }
}
