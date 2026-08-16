package com.aicustomer.websocket;

import com.aicustomer.constant.SubjectType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 连接池，按身份类型（USER / TENANT）分组管理活跃连接。
 * 同一个身份只保留一条最新连接，新连接入池时自动关闭旧连接。
 */
@Slf4j
@Component
public class ConnectionPool {

    private final Map<Long, ChatConnection> tenantConnections = new ConcurrentHashMap<>();
    private final Map<Long, ChatConnection> userConnections = new ConcurrentHashMap<>();

    /**
     * 将连接放入连接池。如果该身份已有旧连接，先将其主动关闭，再存入新连接。
     *
     * @param type       身份类型（USER 或 TENANT）
     * @param subjectId  身份 ID
     * @param connection 要存入的连接
     */
    public void put(SubjectType type, Long subjectId, ChatConnection connection) {
        Map<Long, ChatConnection> pool = poolOf(type);

        ChatConnection existing = pool.get(subjectId);
        if (existing != null && existing != connection) {
            // 关闭旧连接（code 4001 表示"被新连接顶替"）
            existing.close(4001, "duplicate connection");
        }
        pool.put(subjectId, connection);
    }

    /**
     * 根据身份类型和 ID 获取对应连接。
     *
     * @param type      身份类型
     * @param subjectId 身份 ID
     * @return 对应连接，若不存在返回 null
     */
    public ChatConnection get(SubjectType type, Long subjectId) {
        return poolOf(type).get(subjectId);
    }

    /**
     * 从连接池中移除指定连接并返回被移除的连接。
     *
     * @param type      身份类型
     * @param subjectId 身份 ID
     * @return 被移除的连接，若不存在返回 null
     */
    public ChatConnection remove(SubjectType type, Long subjectId) {
        return poolOf(type).remove(subjectId);
    }

    /**
     * 获取指定身份类型的全部连接快照。
     *
     * @param type 身份类型
     * @return 该类型对应的连接 Map
     */
    public Map<Long, ChatConnection> allConnectionsOf(SubjectType type) {
        return poolOf(type);
    }

    /**
     * 获取指定身份类型的连接数量。
     *
     * @param type 身份类型
     * @return 当前连接数
     */
    public int sizeOf(SubjectType type) {
        return poolOf(type).size();
    }

    /**
     * 根据身份类型返回对应的连接 Map。
     *
     * @param type 身份类型
     * @return 对应的连接 Map
     */
    private Map<Long, ChatConnection> poolOf(SubjectType type) {
        return type == SubjectType.TENANT ? tenantConnections : userConnections;
    }
}
