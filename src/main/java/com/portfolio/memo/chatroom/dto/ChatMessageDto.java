package com.portfolio.memo.chatroom.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
// 클라이언트 -> 서버로 메시지 전송에 사용
public class ChatMessageDto {
    private String type;
    private Long roomId;
    private String sender;
    private String message;
    private String sentAt;


    // unreadStatus 정보 같이 전송
    // unreadCount = 채팅방 총 인원 - readStatus Set의 크기
    private Long messageId;
    private int unreadCount; // 안 읽은 사람 수

    @Builder
    public ChatMessageDto(String type, Long roomId, String sender, String message, Long messageId, int unreadCount) {
        this.type = type;
        this.roomId = roomId;
        this.messageId = messageId;
        this.sender = sender;
        this.message = message;
        this.unreadCount = unreadCount;
    }
}
