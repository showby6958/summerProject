package com.portfolio.memo.chatroom.message.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReadMessageResponse {
    private Long messageId;
    private Long roomId;
    private Long userId;
    private int unreadCount;
}
