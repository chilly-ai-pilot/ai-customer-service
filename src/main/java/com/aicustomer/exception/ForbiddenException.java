package com.aicustomer.exception;

import com.aicustomer.constant.ErrorCodes;

public class ForbiddenException extends BusinessException {
    public ForbiddenException() {
        super(ErrorCodes.FORBIDDEN, ErrorCodes.MSG_FORBIDDEN);
    }
}
