package com.aicustomer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录请求。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "登录请求")
public class LoginRequest {

    @NotBlank(message = "账号不能为空")
    @Schema(description = "账号", example = "user@example.com")
    private String account;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码", example = "Pass123456")
    private String password;
}
