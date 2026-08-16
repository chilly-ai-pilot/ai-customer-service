package com.aicustomer.websocket.task;

import com.aicustomer.constant.SubjectType;
import com.aicustomer.service.TokenService;
import com.aicustomer.websocket.ChatConnection;
import com.aicustomer.websocket.ConnectionPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class TokenValidationTask {

    private static final int CHECK_INTERVAL_MS = 30_000;

    @Autowired
    private ConnectionPool connectionPool;

    @Autowired
    private TokenService tokenService;

    @Scheduled(fixedRate = CHECK_INTERVAL_MS)
    public void validateTokens() {
        for (SubjectType type : SubjectType.values()) {
            Map<Long, ChatConnection> pool = connectionPool.allConnectionsOf(type);
            for (Map.Entry<Long, ChatConnection> entry : pool.entrySet()) {
                Long subjectId = entry.getKey();
                ChatConnection conn = entry.getValue();

                if (!conn.isOpen()) {
                    connectionPool.remove(type, subjectId);
                    continue;
                }

                String token = conn.getToken();
                Long resolved = tokenService.resolve(type, token);
                if (resolved == null || !resolved.equals(subjectId)) {
                    log.info("Token expired/invalid for {} {}, closing connection", type, subjectId);
                    conn.close(4004, "token expired");
                    connectionPool.remove(type, subjectId);
                }
            }
        }
    }
}
