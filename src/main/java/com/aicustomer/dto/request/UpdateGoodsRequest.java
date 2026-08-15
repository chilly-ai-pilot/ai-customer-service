package com.aicustomer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "更新商品请求")
public class UpdateGoodsRequest {

    @NotNull(message = "商品ID不能为空")
    @Schema(description = "商品ID", example = "1")
    private Long id;

    @NotBlank(message = "商品名称不能为空")
    @Schema(description = "商品名称", example = "更新后的商品名称")
    private String name;
}
