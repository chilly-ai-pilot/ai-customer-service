package com.aicustomer.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工作台菜单项响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "菜单项")
public class MenuItemResponse {

    @Schema(description = "菜单名称", example = "商品管理")
    private String name;

    @Schema(description = "菜单路由", example = "/goods")
    private String path;

    @Schema(description = "是否为占位模块（占位模块暂不可点击）", example = "false")
    private boolean placeholder;
}
