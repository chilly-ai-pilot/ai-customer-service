package com.aicustomer.websocket;

import com.aicustomer.constant.SubjectType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ConnectionPool {

    private final Map<Long, ChatConnection> tenantConnections = new ConcurrentHashMap<>();
    private final Map<Long, ChatConnection> userConnections = new ConcurrentHashMap<>();

    public void put(SubjectType type, Long subjectId, ChatConnection connection) {
        Map<Long, ChatConnection> pool = poolOf(type);

        ChatConnection existing = pool.get(subjectId);
        if (existing != null && existing != connection) {
            existing.close(4001, "duplicate connection");
        }
        pool.put(subjectId, connection);
    }

    public ChatConnection get(SubjectType type, Long subjectId) {
        return poolOf(type).get(subjectId);
    }

    public ChatConnection remove(SubjectType type, Long subjectId) {
        return poolOf(type).remove(subjectId);
    }

    public Map<Long, ChatConnection> allConnectionsOf(SubjectType type) {
        return poolOf(type);
    }

    public int sizeOf(SubjectType type) {
        return poolOf(type).size();
    }

    private Map<Long, ChatConnection> poolOf(SubjectType type) {
        return type == SubjectType.TENANT ? tenantConnections : userConnections;
    }
}
