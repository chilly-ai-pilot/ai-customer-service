package com.aicustomer.exception;

import com.aicustomer.constant.ErrorCodes;

public class ParameterErrorException extends BusinessException {

    public ParameterErrorException(String message) {
        super(ErrorCodes.PARAMETER_ERROR, message);
    }
}
