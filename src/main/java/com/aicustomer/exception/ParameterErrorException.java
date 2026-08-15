package com.aicustomer.exception;

public class ParameterErrorException extends BusinessException {

    public ParameterErrorException(String message) {
        super(1004, message);
    }
}
