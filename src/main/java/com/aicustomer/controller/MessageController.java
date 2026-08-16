package com.aicustomer.controller;

import com.aicustomer.context.CurrentUser;
import com.aicustomer.dto.response.ApiResponse;
import com.aicustomer.dto.response.MessageResponse;
import com.aicustomer.entity.Session;
import com.aicustomer.service.MessageService;
import com.aicustomer.service.SessionAccessService;
import com.aicustomer.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 消息管理 Controller，提供消息列表查询、已读标记、未读数统计接口。
 */
@RestController
@RequestMapping("/session/{sessionId}/message")
@Tag(name = "消息管理", description = "聊天消息查询与已读操作")
public class MessageController {

    private final TokenService tokenService;
    private final SessionAccessService sessionAccess;
    private final MessageService messageService;

    public MessageController(TokenService tokenService,
                          SessionAccessService sessionAccess,
                          MessageService messageService) {
        this.tokenService = tokenService;
        this.sessionAccess = sessionAccess;
        this.messageService = messageService;
    }

    /**
     * 查询会话消息列表（按时间正序）。
     * 归属校验：只有会话参与方才能查看。
     */
    @Operation(summary = "查询消息列表")
    @GetMapping("/list")
    public ApiResponse<Page<MessageResponse>> list(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "50") int pageSize) {
        CurrentUser currentUser = tokenService.resolveCurrentUser(token);
        Session session = sessionAccess.validateAndGetSession(sessionId, currentUser);
        Page<MessageResponse> page = messageService.listMessages(session, pageNum, pageSize);
        return ApiResponse.success(page);
    }

    /**
     * 标记会话消息为已读。
     * 归属校验：只有会话参与方才能操作。
     */
    @Operation(summary = "标记已读")
    @PutMapping("/read")
    public ApiResponse<Map<String, Object>> markRead(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long sessionId) {
        CurrentUser currentUser = tokenService.resolveCurrentUser(token);
        Session session = sessionAccess.validateAndGetSession(sessionId, currentUser);
        Map<String, Object> result = messageService.markRead(session, currentUser);
        return ApiResponse.success(result);
    }

    /**
     * 查询未读消息数。
     * 归属校验：只有会话参与方才能查看。
     */
    @Operation(summary = "未读消息数")
    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Object>> unreadCount(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long sessionId) {
        CurrentUser currentUser = tokenService.resolveCurrentUser(token);
        Session session = sessionAccess.validateAndGetSession(sessionId, currentUser);
        long count = messageService.countUnread(session, currentUser);
        return ApiResponse.success(Map.of("sessionId", sessionId, "unreadCount", count));
    }
}
