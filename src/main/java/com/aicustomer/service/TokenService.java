package com.aicustomer.service;

import com.aicustomer.context.CurrentUser;
import com.aicustomer.constant.SubjectType;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token 管理服务，负责 token 的生成和解析。
 * 采用内存存储（生产环境建议替换为 Redis）。
 */
@Service
public class TokenService {

    /** 按身份类型分组的 token 存储：token -> subjectId */
    private final Map<SubjectType, ConcurrentHashMap<String, Long>> tokenStores =
            new EnumMap<>(SubjectType.class);

    public TokenService() {
        for (SubjectType type : SubjectType.values()) {
            tokenStores.put(type, new ConcurrentHashMap<>());
        }
    }

    /**
     * 为指定身份生成新 token。
     * 新 token 生成时会清除该身份此前持有的旧 token（如果有），保证同一身份同时只有一份有效 token。
     *
     * @param type      身份类型
     * @param subjectId 身份 ID
     * @return 新生成的 token
     */
    public String generateToken(SubjectType type, Long subjectId) {
        ConcurrentHashMap<String, Long> store = tokenStores.get(type);
        // 生成新 token 并清除该身份之前的旧 token
        String newToken = UUID.randomUUID().toString().replace("-", "");
        store.entrySet().removeIf(e -> e.getValue().equals(subjectId));
        store.put(newToken, subjectId);
        return newToken;
    }

    /**
     * 解析 token 对应的身份 ID。
     *
     * @param type  身份类型
     * @param token token 值
     * @return 身份 ID，若 token 无效或已过期返回 null
     */
    public Long resolve(SubjectType type, String token) {
        if (token == null) {
            return null;
        }
        return tokenStores.get(type).get(token);
    }

    /**
     * 严格鉴权：从 token 解析当前登录身份。
     *
     * @param token token 值
     * @return 当前登录身份
     * @throws com.aicustomer.exception.UnauthorizedException token 为空或无效
     */
    public CurrentUser resolveCurrentUser(String token) {
        if (token == null || token.isBlank()) {
            throw new com.aicustomer.exception.UnauthorizedException();
        }
        Long ctId = tokenStores.get(SubjectType.TENANT).get(token);
        if (ctId != null) {
            return new CurrentUser(SubjectType.TENANT, ctId);
        }
        Long userId = tokenStores.get(SubjectType.USER).get(token);
        if (userId != null) {
            return new CurrentUser(SubjectType.USER, userId);
        }
        throw new com.aicustomer.exception.UnauthorizedException();
    }

    /**
     * 强制校验：要求 token 属于指定类型，不接受跨类型 token。
     *
     * @param type  期望的身份类型
     * @param token token 值
     * @return 身份 ID
     * @throws com.aicustomer.exception.UnauthorizedException token 为空或不属于该类型
     */
    public Long requireToken(SubjectType type, String token) {
        if (token == null || token.isBlank()) {
            throw new com.aicustomer.exception.UnauthorizedException();
        }
        Long id = tokenStores.get(type).get(token);
        if (id == null) {
            throw new com.aicustomer.exception.UnauthorizedException();
        }
        return id;
    }
}
