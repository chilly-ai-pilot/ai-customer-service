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

@Slf4j
@Component
public class ChatHandshakeInterceptor implements HandshakeInterceptor {

    @Autowired
    private TokenService tokenService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                  WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        String path = request.getURI().getPath();

        String token = extractToken(request);
        if (token == null || token.isBlank()) {
            log.warn("Handshake rejected: missing token");
            return false;
        }

        Long resolvedUserId = tokenService.resolve(SubjectType.USER, token);
        Long resolvedCtId = tokenService.resolve(SubjectType.TENANT, token);

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
            attributes.put("subjectId", resolvedCtId);
            attributes.put("subjectType", SubjectType.TENANT);
            attributes.put("token", token);
            return true;
        }

        log.warn("Handshake rejected: unknown path {}", path);
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }

    private String extractToken(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            var httpSession = servletRequest.getServletRequest().getSession(false);
            if (httpSession != null) {
                Object token = httpSession.getAttribute("token");
                if (token instanceof String) {
                    return (String) token;
                }
            }
        }
        return null;
    }

    private Long extractPathId(String path, String prefix) {
        String idPart = path.substring(prefix.length());
        int slashIdx = idPart.indexOf('/');
        if (slashIdx > 0) {
            idPart = idPart.substring(0, slashIdx);
        }
        return Long.parseLong(idPart);
    }
}
