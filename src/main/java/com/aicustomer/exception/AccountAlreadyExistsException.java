package com.aicustomer.exception;

public class AccountAlreadyExistsException extends BusinessException {

    public AccountAlreadyExistsException() {
        super(1002, "账号已存在");
    }
}
