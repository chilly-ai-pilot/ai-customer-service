package com.aicustomer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 添加商品请求。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "添加商品请求")
public class AddGoodsRequest {

    @NotBlank(message = "商品名称不能为空")
    @Schema(description = "商品名称", example = "测试商品")
    private String name;
}
