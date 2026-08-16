package com.aicustomer.config;

import com.aicustomer.websocket.CommercialTenantChatHandler;
import com.aicustomer.websocket.UserChatHandler;
import com.aicustomer.websocket.config.ChatHandshakeInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private UserChatHandler userChatHandler;

    @Autowired
    private CommercialTenantChatHandler commercialTenantChatHandler;

    @Autowired
    private ChatHandshakeInterceptor chatHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(userChatHandler, "/user/chat/{userId}")
                .addInterceptors(chatHandshakeInterceptor)
                .setAllowedOrigins("*");
        registry.addHandler(commercialTenantChatHandler, "/commercialTenant/chat/{ctId}")
                .addInterceptors(chatHandshakeInterceptor)
                .setAllowedOrigins("*");
    }
}
