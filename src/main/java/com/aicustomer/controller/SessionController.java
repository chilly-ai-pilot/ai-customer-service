package com.aicustomer.controller;

import com.aicustomer.context.CurrentUser;
import com.aicustomer.constant.SubjectType;
import com.aicustomer.dto.response.ApiResponse;
import com.aicustomer.dto.response.SessionResponse;
import com.aicustomer.entity.Session;
import com.aicustomer.service.SessionAccessService;
import com.aicustomer.service.SessionService;
import com.aicustomer.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/session")
@Tag(name = "会话管理", description = "用户与商户的聊天会话管理")
public class SessionController {

    private final TokenService tokenService;
    private final SessionService sessionService;
    private final SessionAccessService sessionAccess;

    public SessionController(TokenService tokenService,
                          SessionService sessionService,
                          SessionAccessService sessionAccess) {
        this.tokenService = tokenService;
        this.sessionService = sessionService;
        this.sessionAccess = sessionAccess;
    }

    @Operation(summary = "商户查询会话列表")
    @GetMapping("/ct/list")
    public ApiResponse<Page<SessionResponse>> listByTenant(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long ctId = tokenService.requireToken(SubjectType.TENANT, token);
        Page<SessionResponse> page = sessionService.listByTenant(ctId, pageNum, pageSize);
        return ApiResponse.success(page);
    }

    @Operation(summary = "用户查询会话列表")
    @GetMapping("/user/list")
    public ApiResponse<Page<SessionResponse>> listByUser(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = tokenService.requireToken(SubjectType.USER, token);
        Page<SessionResponse> page = sessionService.listByUser(userId, pageNum, pageSize);
        return ApiResponse.success(page);
    }

    @Operation(summary = "查询会话详情")
    @GetMapping("/{sessionId}")
    public ApiResponse<SessionResponse> detail(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long sessionId) {
        CurrentUser currentUser = tokenService.resolveCurrentUser(token);
        Session session = sessionAccess.validateAndGetSession(sessionId, currentUser);
        return ApiResponse.success(sessionService.toResponse(session));
    }
}
