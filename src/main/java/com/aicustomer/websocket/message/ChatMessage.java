package com.aicustomer.websocket.message;

import com.aicustomer.constant.SubjectType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessage {

    private String messageId;

    private State state;

    private String content;

    /** 会话ID，由用户首条消息时由服务端填充，后续消息由客户端传入 */
    private Long sessionId;

    private Long senderId;

    private SubjectType senderType;

    /** 接收方ID */
    private Long receiverId;

    private SubjectType receiverType;

    /** 商品ID */
    private Long goodsId;

    /** 用户发消息时指定目标商户ID */
    private Long ctId;

    private Instant timestamp;

    public enum State {
        /** Sender side: request sent */
        SENT,
        /** Server -> sender: message delivered */
        SUCCESS,
        /** Server -> sender: new session created and message delivered */
        SESSION_CREATED,
        /** Server -> sender: error occurred */
        ERROR,
        /** Server -> receiver: forward the message */
        DELIVERED,
        /** Receiver -> server: message received acknowledgment */
        RECEIVED
    }
}
