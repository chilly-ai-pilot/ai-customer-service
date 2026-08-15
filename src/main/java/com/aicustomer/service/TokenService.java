package com.aicustomer.service;

import com.aicustomer.constant.SubjectType;
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
        return tokenStores.get(type).get(token);
    }
}
