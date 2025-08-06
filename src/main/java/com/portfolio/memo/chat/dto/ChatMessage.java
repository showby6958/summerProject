package com.portfolio.memo.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
// 클라이언트는 receiver와 content만 채워서 보내고
// 서버는 sender와 timestamp를 채워서 수신자에게 전달하고, DB에 저장
public class ChatMessage {
    // 클라이언트에서 받을 정보
    private String receiver;
    private String content;

    // 서버에서 추가할 정보
    private String sender;
    private LocalDateTime timestamp;
}
