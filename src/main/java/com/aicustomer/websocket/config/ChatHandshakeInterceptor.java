package com.aicustomer.websocket.config;

import com.aicustomer.constant.SubjectType;
import com.aicustomer.service.TokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手拦截器，负责 WS 连接建立前的身份鉴权。
 *
 * 职责：
 * 1. 从 URL query 参数中提取 token
 * 2. 用 TokenService 解析 token，确认身份
 * 3. 验证"路径参数中的 userId/ctId"与"token 解析出的身份"是否一致
 * 4. 一致则将 subjectId/subjectType/token 注入 session attributes，供 Handler 取用
 * 5. 不一致或 token 无效则在握手阶段直接拒绝连接（onOpen 不会触发）
 *
 * 握手阶段拒绝的好处：不会出现"连上又立刻断"的中间状态，也不需要额外定义关闭码。
 */
@Slf4j
@Component
public class ChatHandshakeInterceptor implements HandshakeInterceptor {

    @Autowired
    private TokenService tokenService;

    /**
     * 握手前回调：执行身份校验和属性注入。
     *
     * 步骤：
     * 1. 从 URL query 提取 token，为空则拒绝
     * 2. 解析 token，尝试 USER 和 TENANT 两种身份
     * 3. 根据路径前缀判断是用户端还是商户端
     * 4. 校验路径中的身份与 token 解析出的身份是否一致，不一致则拒绝
     * 5. 注入 subjectId、subjectType、token 到 session attributes
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                  WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        String path = request.getURI().getPath();

        // 步骤1：从 URL query 提取 token
        String token = extractToken(request);
        if (token == null || token.isBlank()) {
            log.warn("Handshake rejected: missing token");
            return false;
        }

        // 步骤2：解析 token（尝试 USER 和 TENANT 两个 store）
        Long resolvedUserId = tokenService.resolve(SubjectType.USER, token);
        Long resolvedCtId = tokenService.resolve(SubjectType.TENANT, token);

        // 步骤3：根据路径前缀判断类型
        if (path.startsWith("/user/chat/")) {
            if (resolvedUserId == null) {
                log.warn("Handshake rejected: invalid USER token");
                return false;
            }
            Long pathUserId = extractPathId(path, "/user/chat/");
            if (!resolvedUserId.equals(pathUserId)) {
                log.warn("Handshake rejected: USER token subject {} mismatch path user {}", resolvedUserId, pathUserId);
                return false;
            }
            // 步骤5：注入身份信息到 session attributes
            attributes.put("subjectId", resolvedUserId);
            attributes.put("subjectType", SubjectType.USER);
            attributes.put("token", token);
            return true;

        } else if (path.startsWith("/commercialTenant/chat/")) {
            if (resolvedCtId == null) {
                log.warn("Handshake rejected: invalid TENANT token");
                return false;
            }
            Long pathCtId = extractPathId(path, "/commercialTenant/chat/");
            if (!resolvedCtId.equals(pathCtId)) {
                log.warn("Handshake rejected: TENANT token subject {} mismatch path ct {}", resolvedCtId, pathCtId);
                return false;
            }
            // 步骤5：注入身份信息到 session attributes
            attributes.put("subjectId", resolvedCtId);
            attributes.put("subjectType", SubjectType.TENANT);
            attributes.put("token", token);
            return true;
        }

        log.warn("Handshake rejected: unknown path {}", path);
        return false;
    }

    /** 握手后回调：本实现为空 */
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }

    /** 从请求中提取 URL query 参数中的 token */
    private String extractToken(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            return servletRequest.getServletRequest().getParameter("token");
        }
        return null;
    }

    /** 从 URL 路径中提取身份 ID（如 /user/chat/123 -> 123） */
    private Long extractPathId(String path, String prefix) {
        String idPart = path.substring(prefix.length());
        int slashIdx = idPart.indexOf('/');
        if (slashIdx > 0) {
            idPart = idPart.substring(0, slashIdx);
        }
        return Long.parseLong(idPart);
    }
}
