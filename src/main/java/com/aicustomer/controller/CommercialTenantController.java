package com.aicustomer.controller;

import com.aicustomer.dto.request.LoginRequest;
import com.aicustomer.dto.request.RegisterRequest;
import com.aicustomer.dto.response.AccountResponse;
import com.aicustomer.dto.response.ApiResponse;
import com.aicustomer.service.CommercialTenantService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/commercialTenant")
public class CommercialTenantController {

    private final CommercialTenantService commercialTenantService;

    public CommercialTenantController(CommercialTenantService commercialTenantService) {
        this.commercialTenantService = commercialTenantService;
    }

    @PostMapping("/register")
    public ApiResponse<AccountResponse> register(@Valid @RequestBody RegisterRequest request) {
        AccountResponse response = commercialTenantService.register(request);
        return ApiResponse.success(response);
    }

    @PostMapping("/login")
    public ApiResponse<AccountResponse> login(@Valid @RequestBody LoginRequest request) {
        AccountResponse response = commercialTenantService.login(request);
        return ApiResponse.success(response);
    }
}
