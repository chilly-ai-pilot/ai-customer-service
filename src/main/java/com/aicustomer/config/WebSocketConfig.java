package com.aicustomer.config;

import com.aicustomer.websocket.CommercialTenantChatHandler;
import com.aicustomer.websocket.UserChatHandler;
import com.aicustomer.websocket.config.ChatHandshakeInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置：注册用户端和商户端的 WS 处理器及握手拦截器。
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private UserChatHandler userChatHandler;

    @Autowired
    private CommercialTenantChatHandler commercialTenantChatHandler;

    @Autowired
    private ChatHandshakeInterceptor chatHandshakeInterceptor;

    /**
     * 注册两条 WS 路径及对应的 Handler 和拦截器。
     * 允许所有来源（setAllowedOrigins("*")），CORS 配置见 CorsConfig。
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 用户端 WS：/user/chat/{userId}
        registry.addHandler(userChatHandler, "/user/chat/{userId}")
                .addInterceptors(chatHandshakeInterceptor)
                .setAllowedOrigins("*");

        // 商户端 WS：/commercialTenant/chat/{ctId}
        registry.addHandler(commercialTenantChatHandler, "/commercialTenant/chat/{ctId}")
                .addInterceptors(chatHandshakeInterceptor)
                .setAllowedOrigins("*");
    }
}
