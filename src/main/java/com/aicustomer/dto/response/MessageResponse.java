package com.aicustomer.dto.response;

import com.aicustomer.constant.SubjectType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 消息详情响应，对应数据库中 message_t 表的一条记录。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "消息响应")
public class MessageResponse {

    @Schema(description = "数据库主键 ID")
    private Long id;

    @Schema(description = "服务端生成的唯一消息标识")
    private String messageId;

    @Schema(description = "所属会话 ID")
    private Long sessionId;

    @Schema(description = "发送方 ID")
    private Long senderId;

    @Schema(description = "发送方身份类型")
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
