package com.aicustomer.exception;

/**
 * 401 Unauthorized：token 为空或无效，当前请求未通过身份校验。
 */
public class UnauthorizedException extends BusinessException {
    public UnauthorizedException() {
        super(401, "未登录");
    }

    public UnauthorizedException(String message) {
        super(401, message);
    }
}
