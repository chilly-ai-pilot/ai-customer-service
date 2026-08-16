package com.aicustomer.exception;

/**
 * 业务异常基类，携带 HTTP 状态码和错误消息。
 */
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
