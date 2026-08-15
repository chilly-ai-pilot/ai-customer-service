package com.aicustomer.exception;

import com.aicustomer.constant.ErrorCodes;

public class PasswordErrorException extends BusinessException {

    public PasswordErrorException() {
        super(ErrorCodes.PASSWORD_ERROR, ErrorCodes.MSG_PASSWORD_ERROR);
    }
}
