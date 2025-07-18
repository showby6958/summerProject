package com.portfolio.memo.chat;

import com.portfolio.memo.chat.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatService chatService;

    // 클라이언트가 /app/chat/message 로 메시지를 보내면 이 메소드가 처리
    @MessageMapping("/chat/message")
    public void message(ChatMessage message, Principal principal) {
        // 1. 발신자 정보 설정 (JWT 인증을 통해 얻은 Principal 객체 사용)
        String senderEmail = principal.getName();
        message.setSender(senderEmail);

        // 2. 메시지 타임스탬프 설정
        message.setTimestamp(LocalDateTime.now());

        // 3. 메시지를 데이터베이스에 저장
        chatService.saveMessage(message);

        // 4. 수신자에게 메시지 전송
        // /user/{username}/queue/messages 경로로 메시지 전송
        messagingTemplate.convertAndSendToUser(message.getReceiver(), "/queue/messages", message);
    }

    @GetMapping("/api/chat/history/{otherUserEmail}")
    public ResponseEntity<List<ChatMessage>> getMessageHistory(
        @PathVariable String otherUserEmail,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        String currentUserEmail = userDetails.getUsername();
        List<ChatMessage> history = chatService.getMessageHistory(currentUserEmail, otherUserEmail);

        return ResponseEntity.ok(history);
    }
}
