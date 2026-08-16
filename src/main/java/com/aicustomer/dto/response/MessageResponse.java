package com.aicustomer.dto.response;

import com.aicustomer.constant.SubjectType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "消息响应")
public class MessageResponse {

    @Schema(description = "消息ID")
    private Long id;

    @Schema(description = "消息唯一标识")
    private String messageId;

    @Schema(description = "会话ID")
    private Long sessionId;

    @Schema(description = "发送方ID")
    private Long senderId;

    @Schema(description = "发送方类型")
    private SubjectType senderType;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "是否已读")
    private Boolean isRead;

    @Schema(description = "已读时间")
    private Instant readAt;

    @Schema(description = "创建时间")
    private Instant createdAt;
}
