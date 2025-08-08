package com.portfolio.memo.chatroom;

import com.portfolio.memo.auth.User;
import com.portfolio.memo.auth.UserRepository;
import com.portfolio.memo.chatroom.dto.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageReadStatusRepository readStatusRepository;
    private final UserRepository userRepository;

    @Transactional
    public ChatMessageDto markAsRead(Long messageId, String userEmail) {
        // 1. 필수 엔티티 조회
        ChatRoomMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("메세지를 찾지 못했습니다: " + messageId));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다: " + userEmail));

        // 2. 이미 읽었는지 확인하여 중복 저장 방지
        boolean alreadyRead = readStatusRepository.existsByChatMessageAndUser(message, user);

        if (!alreadyRead) {
            // 3. 읽지 않았다면, 새로운 읽음 상태(ChatMessageReadStatus)를 생성하고 저장
            ChatMessageReadStatus readStatus = new ChatMessageReadStatus(message, user);
            readStatusRepository.save(readStatus);
        }

        // 4. 업데이트된 '안 읽은 사람 수' 계산
        // unreadCount = 채팅방 전체 인원 - 메시지 읽은 인원
        int totalParticipants = message.getChatRoom().getParticipants().size();
        int readCount = readStatusRepository.countByChatMessage(message);
        int unreadCount = totalParticipants - readCount;

        // 5. 클라이언트에 전송할 DTO를 생성해서 반환
        return ChatMessageDto.builder()
                .type("READ")
                .messageId(messageId)
                .roomId(message.getChatRoom().getId())
                .unreadCount(unreadCount)
                .build();
    }
}
