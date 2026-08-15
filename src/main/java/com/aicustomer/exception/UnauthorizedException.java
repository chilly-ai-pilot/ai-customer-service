package com.aicustomer.exception;

import com.aicustomer.constant.ErrorCodes;

public class UnauthorizedException extends BusinessException {
    public UnauthorizedException() {
        super(ErrorCodes.UNAUTHORIZED, ErrorCodes.MSG_UNAUTHORIZED);
    }
}
