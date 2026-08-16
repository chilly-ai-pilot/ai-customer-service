package com.aicustomer.exception;

/**
 * 密码错误：登录时输入的密码与数据库中存储的哈希值不匹配。
 */
public class PasswordErrorException extends BusinessException {
    public PasswordErrorException() {
        super(1003, "密码错误");
    }
}
