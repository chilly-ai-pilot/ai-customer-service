package com.aicustomer.exception;

/**
 * 403 Forbidden：当前登录身份无权访问请求的资源。
 */
public class ForbiddenException extends BusinessException {
    public ForbiddenException() {
        super(403, "无权限");
    }

    public ForbiddenException(String message) {
        super(403, message);
    }
}
