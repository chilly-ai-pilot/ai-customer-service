package com.aicustomer.controller;

import com.aicustomer.constant.SubjectType;
import com.aicustomer.dto.request.AddGoodsRequest;
import com.aicustomer.dto.request.UpdateGoodsRequest;
import com.aicustomer.dto.response.ApiResponse;
import com.aicustomer.dto.response.GoodsResponse;
import com.aicustomer.service.GoodsService;
import com.aicustomer.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/goods")
@Tag(name = "商品管理", description = "商户商品管理")
public class GoodsController {

    private final GoodsService goodsService;
    private final TokenService tokenService;

    public GoodsController(GoodsService goodsService, TokenService tokenService) {
        this.goodsService = goodsService;
        this.tokenService = tokenService;
    }

    @Operation(summary = "添加商品")
    @PostMapping("/add")
    public ApiResponse<GoodsResponse> add(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody AddGoodsRequest request) {
        Long ctId = tokenService.requireToken(SubjectType.TENANT, token);
        GoodsResponse response = goodsService.add(ctId, request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "更新商品")
    @PutMapping("/update")
    public ApiResponse<GoodsResponse> update(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody UpdateGoodsRequest request) {
        Long ctId = tokenService.requireToken(SubjectType.TENANT, token);
        GoodsResponse response = goodsService.update(ctId, request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "删除商品")
    @DeleteMapping("/delete")
    public ApiResponse<Void> delete(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam Long id) {
        Long ctId = tokenService.requireToken(SubjectType.TENANT, token);
        goodsService.delete(ctId, id);
        return ApiResponse.success(null);
    }

    @Operation(summary = "我的商品列表")
    @GetMapping("/mine")
    public ApiResponse<Page<GoodsResponse>> mine(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long ctId = tokenService.requireToken(SubjectType.TENANT, token);
        Page<GoodsResponse> page = goodsService.mine(ctId, pageNum, pageSize);
        return ApiResponse.success(page);
    }

    @Operation(summary = "全部商品列表")
    @GetMapping("/all")
    public ApiResponse<Page<GoodsResponse>> all(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Page<GoodsResponse> page = goodsService.all(pageNum, pageSize);
        return ApiResponse.success(page);
    }

    @Operation(summary = "商品详情")
    @GetMapping("/detail")
    public ApiResponse<GoodsResponse> detail(@RequestParam Long id) {
        GoodsResponse response = goodsService.detail(id);
        return ApiResponse.success(response);
    }
}
