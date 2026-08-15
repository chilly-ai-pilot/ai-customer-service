package com.aicustomer.constant;

public final class ErrorCodes {

    private ErrorCodes() {}

    public static final int ACCOUNT_ALREADY_EXISTS = 1001;
    public static final int ACCOUNT_NOT_FOUND = 1002;
    public static final int PASSWORD_ERROR = 1003;
    public static final int PARAMETER_ERROR = 1004;

    public static final String MSG_ACCOUNT_ALREADY_EXISTS = "账号已存在";
    public static final String MSG_ACCOUNT_NOT_FOUND = "账号不存在";
    public static final String MSG_PASSWORD_ERROR = "密码错误";
}
