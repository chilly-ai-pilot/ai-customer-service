package com.aicustomer.service;

import com.aicustomer.context.CurrentUser;
import com.aicustomer.constant.SubjectType;
import com.aicustomer.exception.UnauthorizedException;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenService {

    private final Map<SubjectType, ConcurrentHashMap<String, Long>> tokenStores =
            new EnumMap<>(SubjectType.class);

    public TokenService() {
        for (SubjectType type : SubjectType.values()) {
            tokenStores.put(type, new ConcurrentHashMap<>());
        }
    }

    public String generateToken(SubjectType type, Long subjectId) {
        ConcurrentHashMap<String, Long> store = tokenStores.get(type);
        String newToken = UUID.randomUUID().toString().replace("-", "");
        store.entrySet().removeIf(e -> e.getValue().equals(subjectId));
        store.put(newToken, subjectId);
        return newToken;
    }

    public Long resolve(SubjectType type, String token) {
        if (token == null) {
            return null;
        }
        return tokenStores.get(type).get(token);
    }

    /**
     * 严格鉴权：token 为空或无效均抛异常，返回当前登录身份。
     * 覆盖了旧 resolve 方法的双类型尝试逻辑，调用方更简洁。
     */
    public CurrentUser resolveCurrentUser(String token) {
        if (token == null || token.isBlank()) {
            throw new UnauthorizedException();
        }
        Long ctId = tokenStores.get(SubjectType.TENANT).get(token);
        if (ctId != null) {
            return new CurrentUser(SubjectType.TENANT, ctId);
        }
        Long userId = tokenStores.get(SubjectType.USER).get(token);
        if (userId != null) {
            return new CurrentUser(SubjectType.USER, userId);
        }
        throw new UnauthorizedException();
    }

    /**
     * 仅校验 token 是否属于指定类型，常用于 Controller 层明确要求某类身份的场景。
     */
    public Long requireToken(SubjectType type, String token) {
        if (token == null || token.isBlank()) {
            throw new UnauthorizedException();
        }
        Long id = tokenStores.get(type).get(token);
        if (id == null) {
            throw new UnauthorizedException();
        }
        return id;
    }
}
