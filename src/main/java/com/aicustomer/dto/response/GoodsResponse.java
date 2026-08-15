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
@Schema(description = "商品响应")
public class GoodsResponse {

    @Schema(description = "商品ID", example = "1")
    private Long id;

    @Schema(description = "商品名称", example = "测试商品")
    private String name;

    @Schema(description = "所属商户ID", example = "1")
    private Long ctId;
}
