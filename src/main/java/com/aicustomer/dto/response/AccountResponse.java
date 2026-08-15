package com.aicustomer.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "账号响应")
public class AccountResponse {

    @Schema(description = "用户ID", example = "1")
    private Long id;

    @Schema(description = "账号", example = "user@example.com")
    private String account;

    @Schema(description = "名称", example = "张三")
    private String name;

    @Schema(description = "登录凭证", example = "a1b2c3d4...")
    private String token;
}