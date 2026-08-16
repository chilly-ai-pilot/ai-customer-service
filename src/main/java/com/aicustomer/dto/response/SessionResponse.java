package com.aicustomer.dto.response;

import com.aicustomer.entity.Session;
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
@Schema(description = "会话响应")
public class SessionResponse {

    @Schema(description = "会话ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "商户ID")
    private Long ctId;

    @Schema(description = "商品ID")
    private Long goodsId;

    @Schema(description = "会话状态")
    private Session.ConversationStatus conversationStatus;

    @Schema(description = "最后消息时间")
    private Instant lastMessageTime;

    @Schema(description = "创建时间")
    private Instant createdAt;

    @Schema(description = "最后一条消息内容（预览）")
    private String lastMessageContent;

    @Schema(description = "当前用户在该会话中的未读消息数")
    private Long unreadCount;
}
