package com.portfolio.memo.chat.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.memo.chat.message.dto.ChatMessageDto;
import com.portfolio.memo.chat.message.dto.EditMessageRequest;
import com.portfolio.memo.chat.message.dto.SendMessageRequest;
import com.portfolio.memo.chat.message.dto.SendMessageResponse;
import com.portfolio.memo.chat.outbox.domain.OutboxEvent;
import com.portfolio.memo.chat.outbox.domain.OutboxEventRepository;
import com.portfolio.memo.chat.outbox.domain.OutboxEventType;
import com.portfolio.memo.chat.outbox.domain.event.MessageCreatedEvent;
import com.portfolio.memo.chat.participant.ParticipantValidator;
import com.portfolio.memo.chat.room.dto.ChatHistoryMessageDto;
import com.portfolio.memo.common.jwt.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ParticipantValidator participantValidator;
    private final ChatMessageCacheWriter cacheWriter;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate chatRedisTemplate;

    private static final long CACHE_TTL_HOURS = 12; // 캐시 만료 시간


    // 전송한 메시지를 저장하는 api
    @Transactional
    public SendMessageResponse send(Long roomId, SendMessageRequest request, @AuthenticationPrincipal CustomUserPrincipal currentUser) {

        // 1. 권한 검증
        participantValidator.validateParticipant(roomId, currentUser.getUserId());

        // 2. DB 저장
        ChatMessage saved = chatMessageRepository.save(
                ChatMessage.builder()
                        .roomId(roomId)
                        .senderId(currentUser.getUserId())
                        .senderName(currentUser.getUsername())
                        .content(request.getContent())
                        .build()
        );

        // 3. Outbox 이벤트 기록 (같은 트랜잭션)
        MessageCreatedEvent event = MessageCreatedEvent.from(saved);

        OutboxEvent outbox = OutboxEvent.pending(
                event.getEventId(),
                event.getEventType(),
                "ChatMessage",
                saved.getId(),
                toJson(event)
        );

        outboxEventRepository.save(outbox);

        // 4. Redis 캐시에 저장
        cacheWriter.cache(saved);

        return SendMessageResponse.from(saved);
    }


    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }


    @Transactional
    public ChatMessageDto editMessage(Long roomId, Long messageId, EditMessageRequest editReq, CustomUserPrincipal currentUser) {
        // 1. 메시지 (존재여부) 조회
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Cannot found message. messageId: {}" + messageId));

        // 2 roomId 정합성 검증 (URL로 받은 roomId와 실제 메시지 roomId가 다르면 차단)
        if (!message.getRoomId().equals(roomId)) {
            throw new IllegalArgumentException("Invalid request. roomId mismatch.");
        }

        // 3. 삭제된 메시지는 수정 불가능
        if (message.isDeleted()) {
            throw new IllegalStateException("Deleted message cannot be edited.");
        }

        // 4. 수정 권한 확인 (작성자 == 요청자)
        if (!message.getSenderId().equals(currentUser.getUserId())) {
            throw new MessageEditNotAllowedException(messageId, currentUser.getUserId());
        }

        // 5. 내용/수정시간 업데이트
        message.editContent(editReq.getNewContent());
        chatMessageRepository.save(message);

        ChatMessageDto dto = ChatMessageDto.from(message);
        dto.setType("EDIT");

        // 캐시 갱신 (삭제 후 같은 위치에 재캐싱)
        refreshEditedMessageCache(message);

        return dto;
    }


    private void refreshEditedMessageCache(ChatMessage updatedMessage) {

        String zsetKey = zsetKey(updatedMessage.getRoomId());
        String messageKey = messageKey(updatedMessage.getId());

        // 같은 위치 유지: score는 sentAt 기반 (수정은 sentAt 변경 X)
        double score = toEpochMilliScore(updatedMessage.getSentAt());

        // 캐시 저장 DTO는 ChatHistoryMessageDto로 통일
        String json;
        try {
            // 1. 메시지 DTO -> JSON 변환
            ChatHistoryMessageDto cacheDto = ChatHistoryMessageDto.from(updatedMessage);
            json = objectMapper.writeValueAsString(cacheDto);
        } catch (JsonProcessingException e) {
            log.error("Edited message serialize failed. messageId: {}", updatedMessage.getId(), e);
            return ;
        }

        chatRedisTemplate.executePipelined(new SessionCallback<Void>() {
            @Override
            public Void execute(RedisOperations operations) {
                // 1. 기존 캐시 제거
                operations.delete(messageKey);

                // 2. 수정된 메시지 재캐싱 (TTL 포함)
                operations.opsForValue().set(messageKey, json, CACHE_TTL_HOURS, TimeUnit.HOURS);

                // 3. zset에 같은 member를 같은 score로 다시 넣음 - 이미있어도 score 동일이면 위치 변화 없음
                operations.opsForZSet().add(zsetKey, messageKey, score);

                // 4. 방 키 TTL 갱신
                operations.expire(zsetKey, CACHE_TTL_HOURS, TimeUnit.HOURS);

                return null;
            }
        });
    }


    @Transactional
    public ChatMessageDto deleteMessage(Long roomId, Long messageId, CustomUserPrincipal currentUser) {
        // 1. 메시지 조회
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Cannot found message. messageId: {}" + messageId));

        // 2. roomId 정합성 체크
        if (!message.getRoomId().equals(roomId)) {
            throw new IllegalArgumentException("Invalid request. roomId mismatch.");
        }

        // 3. 삭제 권한 확인(메시지 작성자와 요청자가 동일한지)
        if (!message.getSenderId().equals(currentUser.getUserId())) {
            throw new MessageDeleteNotAllowed(messageId, currentUser.getUserId());
        }

        // 4. Soft Delete 방식
        message.softDelete();

        chatMessageRepository.save(message);

        // 5. Redis 캐시 원문 제거: "삭제된 메시지입니다." DTO로 덮어쓰기
        cacheDeletedPlaceholder(message);

        // 6. 브로드캐스트/응답 DTO 구성 (삭제 표시)
        ChatMessageDto dto = ChatMessageDto.from(message);
        dto.setType("DELETE");

        return dto;
    }

    public void cacheDeletedPlaceholder(ChatMessage deleted) {
        String zsetKey = "chat:room:" + deleted.getRoomId() + ":messages";
        String messageKey = "chat:message:" + deleted.getId();

        double score = toEpochMilliScore(deleted.getSentAt());

        String json;
        try {
            // ChatHistoryMessageDto로 통일 + content가 "삭제된 메시지입니다"로 치환
            ChatHistoryMessageDto cacheDto = ChatHistoryMessageDto.deletedPlaceholder(deleted);
            json = objectMapper.writeValueAsString(cacheDto);
        } catch (JsonProcessingException e) {
            log.error("Serialize failed for deleted placeholder. messageId: {}", deleted.getId(), e);
            return;
        }

        chatRedisTemplate.executePipelined(new SessionCallback<Void>() {
            @Override
            public Void execute(RedisOperations operations) {
                // 원문 제거 효과: content 덮어쓰기 (원문 JSON이 캐시에 남지 않음)
                operations.opsForValue().set(messageKey, json, CACHE_TTL_HOURS, TimeUnit.HOURS);

                // 순서 유지
                operations.opsForZSet().add(zsetKey, messageKey, score);

                // 만료시간 갱신
                operations.expire(zsetKey, CACHE_TTL_HOURS, TimeUnit.HOURS);

                return null;
            }
        });
    }


    private String zsetKey(Long roomId) {
        return "chat:room:" + roomId + ":messages";
    }
    private String messageKey(Long messageId) {
        return "chat:message:" + messageId;
    }

    private double toEpochMilliScore(LocalDateTime sentAt) {
        long epochMilli = sentAt.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();
        return (double) epochMilli;
    }

}


