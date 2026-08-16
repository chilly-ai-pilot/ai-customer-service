package com.aicustomer.constant;

/**
 * 错误码常量定义。
 */
public final class ErrorCodes {

    private ErrorCodes() {}

    /** 账号已存在 */
    public static final int ACCOUNT_ALREADY_EXISTS = 1001;
    /** 账号不存在 */
    public static final int ACCOUNT_NOT_FOUND = 1002;
    /** 密码错误 */
    public static final int PASSWORD_ERROR = 1003;
    /** 参数错误 */
    public static final int PARAMETER_ERROR = 1004;

    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
}
