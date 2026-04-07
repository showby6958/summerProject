package com.portfolio.memo.chat.outbox.messaging.payload;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MessageCreatedPayload {

    private Long roomId;
    private Long messageId;
    private Long senderId;
    private String senderName;
    private String content;

    private List<Long> recipientUserIds; // 현재 참여자
}
