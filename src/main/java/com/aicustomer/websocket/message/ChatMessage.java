package com.aicustomer.websocket.message;

import com.aicustomer.constant.SubjectType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * WebSocket 消息体，在用户端与商户端之间双向传输。
 * 由服务端生成 messageId，服务端填充 timestamp，保证时间戳权威性。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessage {

    /** 消息唯一标识，由服务端生成；客户端发送时可附带 tempId 供前端追踪 */
    private String messageId;

    /** 消息状态，标识该消息的类型或服务端处理结果 */
    private State state;

    /** 消息文本内容 */
    private String content;

    /** 会话 ID，由服务端在首条消息时填充，后续消息由客户端传入 */
    private Long sessionId;

    /** 发送方 ID */
    private Long senderId;

    /** 发送方身份类型 */
    private SubjectType senderType;

    /** 接收方 ID */
    private Long receiverId;

    /** 接收方身份类型 */
    private SubjectType receiverType;

    /** 商品 ID（用户发消息时携带，用于建立会话） */
    private Long goodsId;

    /** 商户 ID（用户发消息时携带，用于建立会话） */
    private Long ctId;

    /** 消息时间戳（服务端权威时间） */
    private Instant timestamp;

    /**
     * 消息状态枚举，定义消息在流转各阶段的语义。
     */
    public enum State {
        /** 客户端已发送（本地状态，不在服务端流转） */
        SENT,
        /** 服务端 -> 发送方：消息已落库成功 */
        SUCCESS,
        /** 服务端 -> 发送方：新建会话并消息已落库 */
        SESSION_CREATED,
        /** 服务端 -> 发送方：发生错误 */
        ERROR,
        /** 服务端 -> 接收方：转发消息 */
        DELIVERED,
        /** 接收方 -> 服务端：已收到消息 */
        RECEIVED
    }
}
