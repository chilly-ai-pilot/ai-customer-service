package com.aicustomer.exception;

import com.aicustomer.constant.ErrorCodes;

public class AccountNotFoundException extends BusinessException {

    public AccountNotFoundException() {
        super(ErrorCodes.ACCOUNT_NOT_FOUND, ErrorCodes.MSG_ACCOUNT_NOT_FOUND);
    }
}
