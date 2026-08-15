package com.aicustomer.controller;

import com.aicustomer.constant.SubjectType;
import com.aicustomer.dto.response.ApiResponse;
import com.aicustomer.dto.response.MenuItemResponse;
import com.aicustomer.exception.UnauthorizedException;
import com.aicustomer.service.TokenService;
import com.aicustomer.service.WorkbenchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/workbench")
@Tag(name = "工作台", description = "商户工作台")
public class WorkbenchController {

    private final WorkbenchService workbenchService;
    private final TokenService tokenService;

    public WorkbenchController(WorkbenchService workbenchService, TokenService tokenService) {
        this.workbenchService = workbenchService;
        this.tokenService = tokenService;
    }

    @Operation(summary = "工作台菜单")
    @GetMapping("/menu")
    public ApiResponse<List<MenuItemResponse>> menu(
            @RequestHeader(value = "Authorization", required = false) String token) {
        if (token == null || token.isBlank()) {
            throw new UnauthorizedException();
        }
        if (tokenService.resolve(SubjectType.TENANT, token) == null) {
            throw new UnauthorizedException();
        }
        List<MenuItemResponse> menu = workbenchService.getMenu();
        return ApiResponse.success(menu);
    }
}
