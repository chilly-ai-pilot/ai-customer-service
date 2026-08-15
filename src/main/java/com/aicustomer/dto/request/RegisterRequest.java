package com.aicustomer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "注册请求")
public class RegisterRequest {

    @NotBlank(message = "账号不能为空")
    @Schema(description = "账号", example = "user@example.com")
    private String account;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码", example = "Pass123456")
    private String password;

    @NotBlank(message = "名称不能为空")
    @Schema(description = "名称", example = "张三")
    private String name;
}