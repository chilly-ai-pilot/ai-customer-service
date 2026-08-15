package com.aicustomer.exception;

public class PasswordErrorException extends BusinessException {

    public PasswordErrorException() {
        super(1003, "密码错误");
    }
}
