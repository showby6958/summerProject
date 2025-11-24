package com.portfolio.memo.chatroom.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.memo.auth.CustomUserDetails;
import com.portfolio.memo.auth.User;
import com.portfolio.memo.auth.UserRepository;
import com.portfolio.memo.chatroom.message.dto.ChatMessageDto;
import com.portfolio.memo.chatroom.message.dto.ChatMessageHistoryDto;
import com.portfolio.memo.chatroom.message.dto.MessageEditRequestDto;
import com.portfolio.memo.chatroom.room.ChatRoom;
import com.portfolio.memo.chatroom.room.ChatRoomRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;import org.springframework.data.redis.core.StringRedisTemplate;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final ChatMessageReadStatusService chatMessageReadStatusService;

    // 메시지 JSON 캐시용
    @Qualifier("chatRedisTemplate")
    private final RedisTemplate<String, String> chatRedisTemplate;

    private static final long CACHE_MAX_SIZE = 100; // 캐시에 저장할 최신 메시지 수


    // 전송한 메시지를 저장하는 api
    @Transactional
    public ChatRoomMessage saveMessage(Long roomId, String senderEmail, String message) {
        User sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다: " + senderEmail));
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("채팅방을 찾을 수 없습니다: " + roomId));

        ChatRoomMessage chatMessage = ChatRoomMessage.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .message(message)
                .build();

        // 1. DB에 메시지 저장
        ChatRoomMessage savedMessage = chatMessageRepository.save(chatMessage);

        // 2. 메시지 Redis 캐싱
        cacheChatMessage(savedMessage);

        // 3. 읽음 상태 캐싱 (unreadCount, readBy Set)
        chatMessageReadStatusService.initializeReadStatus(savedMessage, chatRoom.getParticipants().size());

        // 3. 저장된 엔티티 반환
        return savedMessage;
    }

    private void cacheChatMessage(ChatRoomMessage message) {
        Long roomId = message.getChatRoom().getId();
        Long messageId = message.getId();

        // Redis 키 구성
        String zsetKey = "chat:room:" + roomId + ":messages"; // 메시지 정렬용
        String messageKey = "chat:message:" + messageId; // 실제 메시지 내용 저장
        ChatMessageHistoryDto messageDto = ChatMessageHistoryDto.fromEntity(message);

        try {
            // 1. Dto -> JSON 문자열
            String messageJson = objectMapper.writeValueAsString(messageDto);

            // 2. 개별 메시지 저장(메시지 본문)
            chatRedisTemplate.opsForValue().set(messageKey, messageJson);

            // 3. ZSET에는 messageId만 추가 (score는 생성시간 기준)
            double score = message.getSentAt() != null
                    ? message.getSentAt().toEpochSecond(ZoneOffset.ofHours(9))
                    : messageId.doubleValue();

            chatRedisTemplate.opsForZSet().add(zsetKey, "message:" + messageId, score);

            // 오래된 메시지 캐시 관리
            Long size = chatRedisTemplate.opsForZSet().size(zsetKey);
            if (size != null && size > CACHE_MAX_SIZE) {
                chatRedisTemplate.opsForZSet().removeRange(zsetKey, 0, size - CACHE_MAX_SIZE - 1);
            }

            log.info("Redis 캐시에 메시지 저장 완료. roomId: {}, messageId: {}", roomId, messageId);

        } catch (JsonProcessingException e) {
            log.error("메시지 JSON 직렬화 실패", e);
        }
    }


    @Transactional
    public ChatMessageDto editMessage(Long roomId, Long messageId, MessageEditRequestDto messageEditRequestDto, CustomUserDetails userDetails) {
        // 1. 메시지 (존재여부) 조회
        ChatRoomMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("메시지를 찾을 수 없습니다. ID: " + messageId));

        // 2. 수정권한 확인 (메시지 작성자와 요청자가 동일한지 확인)
        if (!message.getSender().getId().equals(userDetails.getUser().getId())) {
            // Custom Exception 사용
            throw new MessageEditNotAllowedException(messageId, userDetails.getUser().getId());
        }

        // 3. 새 메시지 내용, 수정 시간 업데이트
        message.setMessage(messageEditRequestDto.getNewMessage());
        message.setEditedAt(LocalDateTime.now());

        // 4. DB에 저장 (Transactional 어노테이션으로 인해 메서드 종료 시 자동 저장)
        ChatRoomMessage updatedMessage = chatMessageRepository.save(message);

        // 5. DTO로 변환
        ChatMessageDto updatedMessageDto = ChatMessageDto.from(updatedMessage);
        updatedMessageDto.setType("EDIT"); // 클라이언트가 메시지 수정을 인지하도록 타입 설정

        // 6. 수정된 메시지 정보 반환
        return updatedMessageDto;
    }

    @Transactional
    public ChatMessageDto deleteMessage(Long roomId, Long messageId, CustomUserDetails userDetails) {
        // 1. 메시지 조회
        ChatRoomMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("메시지를 찾을 수 없습니다." + messageId));

        // 2. 삭제 권한 확인(메시지 작성자와 요청자가 동일한지)
        if (!message.getSender().getId().equals(userDetails.getUser().getId())) {
            throw new MessageDeleteNotAllowed(messageId, userDetails.getUser().getId());
        }

        // 3. Soft Delete(DB) 방식
        message.setDeleted(true);
        message.setDeletedAt(LocalDateTime.now());

        // 4. DB에 저장
        ChatRoomMessage deletedMessage = chatMessageRepository.save(message);

        // 1. Redis 키 설정
        String zsetKey = "chat:room:" + roomId + ":messages"; // 메시지 목록용 ZSET
        String messageKey = "chat:room:" + messageId; // 개별 메시지 저장용 Key

        // 2. Redis 캐시에서 메시지 삭제
        chatRedisTemplate.opsForZSet().remove(zsetKey, "message:" + messageId);
        chatRedisTemplate.delete(messageKey);

        log.info("Redis 캐시에서 메시지 삭제 완료. roomId: {}, messageId: {}", roomId, messageId);

        // 3. 클라이언트에게 반환할 DTO 구성
        ChatMessageDto deletedMessageDto = ChatMessageDto.from(deletedMessage);
        deletedMessageDto.setType("DELETE"); // 클라이언트가 메시지 삭제를 인지하도록 타입 설정

        return deletedMessageDto;
    }
}


