package com.aicustomer.exception;

/**
 * 参数错误：请求参数不符合校验规则（如空字段、格式错误等）。
 */
public class ParameterErrorException extends BusinessException {
    public ParameterErrorException() {
        super(1004, "参数错误");
    }

    public ParameterErrorException(String message) {
        super(1004, message);
    }
}
