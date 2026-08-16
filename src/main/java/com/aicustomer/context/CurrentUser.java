package com.aicustomer.context;

import com.aicustomer.constant.SubjectType;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 当前登录身份上下文，封装从 token 解析出的身份类型和 ID。
 */
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
