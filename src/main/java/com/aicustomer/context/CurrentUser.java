package com.aicustomer.context;

import com.aicustomer.constant.SubjectType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CurrentUser {

    private final SubjectType type;
    private final Long id;

    public boolean isTenant() {
        return type == SubjectType.TENANT;
    }

    public boolean isUser() {
        return type == SubjectType.USER;
    }
}
