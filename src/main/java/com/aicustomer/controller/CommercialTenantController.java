package com.aicustomer.controller;

import com.aicustomer.dto.request.LoginRequest;
import com.aicustomer.dto.request.RegisterRequest;
import com.aicustomer.dto.response.AccountResponse;
import com.aicustomer.dto.response.ApiResponse;
import com.aicustomer.service.CommercialTenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 商户端 Controller，提供商户注册、登录、名称查询。
 */
@RestController
@RequestMapping("/commercialTenant")
@Tag(name = "商户", description = "商户注册与登录")
public class CommercialTenantController {

    private final CommercialTenantService commercialTenantService;

    public CommercialTenantController(CommercialTenantService commercialTenantService) {
        this.commercialTenantService = commercialTenantService;
    }

    @Operation(summary = "商户注册")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "注册成功",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    })
    @PostMapping("/register")
    public ApiResponse<AccountResponse> register(@Valid @RequestBody RegisterRequest request) {
        AccountResponse response = commercialTenantService.register(request);
        return ApiResponse.success(response);
    }

    /**
     * 按商户 ID 查询名称，供聊天窗口展示对方名字用。
     * 查不到时返回 null，不抛异常，避免打断聊天页。
     */
    @Operation(summary = "按商户ID查名称（供聊天窗口展示对方名字用）")
    @GetMapping("/{id}/name")
    public ApiResponse<String> name(@PathVariable Long id) {
        return ApiResponse.success(commercialTenantService.getName(id));
    }

    @Operation(summary = "商户登录")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "登录成功",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    })
    @PostMapping("/login")
    public ApiResponse<AccountResponse> login(@Valid @RequestBody LoginRequest request) {
        AccountResponse response = commercialTenantService.login(request);
        return ApiResponse.success(response);
    }
}
