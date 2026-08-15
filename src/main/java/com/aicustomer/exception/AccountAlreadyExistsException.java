package com.aicustomer.exception;

import com.aicustomer.constant.ErrorCodes;

public class AccountAlreadyExistsException extends BusinessException {

    public AccountAlreadyExistsException() {
        super(ErrorCodes.ACCOUNT_ALREADY_EXISTS, ErrorCodes.MSG_ACCOUNT_ALREADY_EXISTS);
    }
}
