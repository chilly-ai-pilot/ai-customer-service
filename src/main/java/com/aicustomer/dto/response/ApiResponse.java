package com.aicustomer.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用 API 响应包装，所有 Controller 接口均返回此类型。
 *
 * @param <T>  data 字段的实际数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "通用响应")
public class ApiResponse<T> {

    @Schema(description = "状态码：0=成功，1001=账号已存在，1002=账号不存在，1003=密码错误，1004=参数错误", example = "0")
    private int code;

    @Schema(description = "响应消息", example = "success")
    private String message;

    @Schema(description = "响应数据")
    private T data;

    /**
     * 构造成功响应。
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "success", data);
    }

    /**
     * 构造错误响应。
     *
     * @param code    错误码
     * @param message 错误消息
     * @param <T>     data 为 null
     * @return 错误响应
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
