package com.portfolio.memo.chat.room;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.memo.chat.message.ChatMessageRepository;
import com.portfolio.memo.chat.message.ChatMessage;
import com.portfolio.memo.chat.participant.ChatRoomParticipant;
import com.portfolio.memo.chat.participant.ChatRoomParticipantRepository;
import com.portfolio.memo.chat.participant.ParticipantValidator;
import com.portfolio.memo.chat.room.dto.ChatHistoryMessageDto;
import com.portfolio.memo.chat.room.dto.ChatRoomResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomParticipantRepository participantRepository;
    private final StringRedisTemplate chatRedisTemplate;
    private final ObjectMapper objectMapper;
    private final ChatMessageRepository chatMessageRepository;
    private final ParticipantValidator participantValidator;

    private static final long CACHE_TTL_HOURS = 12; // 캐시 만료 시간


    // 새 채팅방 생성
    @Transactional
    public ChatRoomResponse createChatRoom(
            String roomName,
            Long creatorUserId,
            String creatorUserName,
            List<Long> participantUserIds
    ) {
        if (roomName == null || roomName.isBlank()) {
            throw new IllegalArgumentException("roomName must not be blank");
        }
        if (creatorUserId == null) {
            throw new IllegalArgumentException("creatorUserId must not be null");
        }

        // 1. ChatRoom 저장
        ChatRoom room = ChatRoom.builder()
                .name(roomName)
                .createdByUserId(creatorUserId)
                .build();

        ChatRoom saved = chatRoomRepository.save(room);


        // 2. 참여자 저장 (요청자는 무조건 포함)
        Set<Long> uniqueUserIds = new LinkedHashSet<>();
        uniqueUserIds.add(creatorUserId);
        if (participantUserIds != null) {
            uniqueUserIds.addAll(participantUserIds);
        }

        // userName은 creator에만 넣고, 초대 유저는 null로 둠
        for (Long uid : uniqueUserIds) {
            String snapName = (uid.equals(creatorUserId) ? creatorUserName : null);
            participantRepository.save(new ChatRoomParticipant(saved.getId(), uid, snapName));
        }

        // 3. 응답
        return ChatRoomResponse.builder()
                .roomId(saved.getId())
                .name(saved.getName())
                .createdAt(saved.getCreatedAt())
                .build();
    }

//    public List<ChatRoomInfo> getChatRoomInfo(Long userId) {
//
//        // 1. 내가 참여한 roomId 목록
//        List<Long> roomIds = participantRepository.findRoomIdsByUserid(userId);
//        if (roomIds.isEmpty()) {
//            return List.of();
//        }
//
//        // 2. roomId로 채팅방 조회
//        List<ChatRoom> rooms = chatRoomRepository.findByIdIn(roomIds);
//
//        참여한 채팅방 조회하는데 DB 조회 너무 많은거 같음 일단 나중에 고치셈
//    }


    // 채팅 기록을 불러오는 api (Redis 캐시 조회 - Cache Aside 방식)
    public List<ChatHistoryMessageDto> getChatHistory(Long roomId, Long userId) {
        // 1. 사용자 및 채팅방 참여 권한 확인
        participantValidator.validateParticipant(roomId, userId);

        String zsetKey = "chat:room:" + roomId + ":messages";

        // 2. Redis 캐시에서 데이터 조회 (Cache-Aside) (ZSET 멤버: "chat:message:{messageId}" )
        Set<String> messageKeysSet = chatRedisTemplate.opsForZSet().range(zsetKey, 0, -1);

        // 3. Cache Hit: Redis에 데이터가 있는 경우,
        // Redis 캐시에 메시지가 있으면 DB를 조회하지 않고 바로 리스트 형태로 반환
        if (messageKeysSet != null && !messageKeysSet.isEmpty()) {
            List<String> messagekeys = new ArrayList<>(messageKeysSet);

            // Cache Hit: multiget + 역직렬화
            List<ChatHistoryMessageDto> cached = readMessagesFromRedis(messagekeys);

            // 캐시가 완전하면 그대로 반환 + TTL 갱신
            if (cached.size() == messagekeys.size()) {
                log.info("Cache Hit for chat room: {}", roomId);
                touchCacheTtl(zsetKey, messagekeys);
                return cached;
            }

            // 일부 누락/역직렬화 실패가 있으면 신뢰하지 않고 DB로 리빌드
            log.warn("Cache Partially Hit (missing/invalid). Rebuilding cache. roomId: {}", roomId);
        } else {
            log.info("Cache Miss for chat room: {}", roomId);
        }

        // 4. Cache Miss: Redis에 데이터가 없는 경우, DB에서 조회 + DB에서 가져온 메시지 캐싱
        return readMessagesFromDbAndCache(roomId, zsetKey);
    }



    // Redis 캐시에서 메시지를 가져옴
    private List<ChatHistoryMessageDto> readMessagesFromRedis(List<String> messageKeysInOrder) {
        List<String> jsonList = chatRedisTemplate.opsForValue().multiGet(messageKeysInOrder);
        if (jsonList == null || jsonList.isEmpty()) {
            return Collections.emptyList();
        }

        List<ChatHistoryMessageDto> result = new ArrayList<>(messageKeysInOrder.size());

        for (int i=0; i < messageKeysInOrder.size(); i++) {
            String json = jsonList.size() > i ? jsonList.get(i) : null;
            if (json == null) continue;

            try {
                result.add(objectMapper.readValue(json, ChatHistoryMessageDto.class));
            } catch (Exception e) {
                log.error("Redis message deserialize failed. key: {}", messageKeysInOrder.get(i), e);
            }
        }

        return result;
    }

    // CacheMiss: DB에서 메시지를 가져옴 + 가져온 메시지 캐싱
    private List<ChatHistoryMessageDto> readMessagesFromDbAndCache(Long roomId, String zsetKey) {
        List<ChatMessage> messagesFromDb = chatMessageRepository.findByRoomIdOrderBySentAtAsc(roomId);

        // DB에서 가져온 메시지 dtos에 저장
        List<ChatHistoryMessageDto> dtos = messagesFromDb.stream()
                .map(ChatHistoryMessageDto::forClient)
                .toList();

        if (!dtos.isEmpty()) {
            cacheMessages(dtos, zsetKey); // DB에서 가져온 메시지 캐싱
        } else {
            chatRedisTemplate.expire(zsetKey, CACHE_TTL_HOURS, TimeUnit.HOURS);
        }

        return dtos;
    }

    private void cacheMessages(List<ChatHistoryMessageDto> dtos, String zsetKey) {
        List<CacheItem> items = new ArrayList<>(dtos.size());
        for (ChatHistoryMessageDto dto : dtos) {
            String messageKey = messageKey(dto.getMessageId());
            try {
                String json = objectMapper.writeValueAsString(dto);
                double score = toEpochMilliScore(dto.getSentAt());
                items.add(new CacheItem(messageKey, json, score));
            } catch (JsonProcessingException e) {
                log.error("Message serialize failed. messageId: {}", dto.getMessageId(), e);
            }

        }

        if (items.isEmpty()) return;

        // 파이프라이닝으로 set + zadd + expire 묶기
        chatRedisTemplate.executePipelined(new SessionCallback<Void>() {
            @Override
            public Void execute(RedisOperations operations) {
                for (CacheItem item : items) {
                    // 개별 messageKey도 TTL을 맞춰줌 (고아 key 방지)
                    operations.opsForValue().set(item.messageKey, item.json, CACHE_TTL_HOURS, TimeUnit.HOURS);
                    operations.opsForZSet().add(zsetKey, item.messageKey, item.score);
                }
                operations.expire(zsetKey, CACHE_TTL_HOURS, TimeUnit.HOURS);
                return null;
            }
        });
    }

    private void touchCacheTtl(String zsetKey, List<String> messageKeys) {
        // Hit된 방은 TTL 갱신
        chatRedisTemplate.executePipelined(new SessionCallback<Void>() {
            @Override
            public Void execute(RedisOperations operations) {
                operations.expire(zsetKey, CACHE_TTL_HOURS, TimeUnit.HOURS);
                for (String mk : messageKeys) {
                    operations.expire(mk, CACHE_TTL_HOURS, TimeUnit.HOURS);
                }
                return null;
            }
        });
    }


    // 채팅방 초대 api
    @Transactional
    public void inviteUsersToChatRoom(Long roomId, List<Long> inviteeUserIds, Long inviterUserId) {
        // 1. roomId로 채팅방이 있는지 조회
        chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Chat room not found: " + roomId));

        // 2. 초대한 사용자가 해당 채팅방의 참여자인지 확인 (권한 확인)
        validateRoomParticipant(roomId, inviterUserId);
        
        // 3. 초대 대상 저장 (중복 참가자는 스킵)
        for (Long inviteeUserId : inviteeUserIds) {
            if (inviteeUserId == null) continue;

            boolean alreadyJoined = participantRepository.existsByRoomIdAndUserId(roomId, inviteeUserId);
            if (alreadyJoined) continue;

            participantRepository.save(new ChatRoomParticipant(roomId, inviteeUserId, null)); // invitee의 이름은 null로 둠, 조회 시점에만 채우는 방식(초대 시점에 외부 호출업음-장애전파 down)
        }
    }

    // 채팅방 참여 권한 확인
    private void validateRoomParticipant(Long roomId, Long userId) {
        // 요청한 사용자가 해당 채팅방의 참여자인지 권한 확인
        boolean isParticipant = participantRepository.existsByRoomIdAndUserId(roomId, userId);

        if (!isParticipant) {
            throw new SecurityException("You do not have permission to access the chat room.");
        }
    }

    private String zsetKey(Long roomId) {
        return "chat:room:" + roomId + ":messages";
    }
    private String messageKey(Long messageId) {
        return "chat:message:" + messageId;
    }

    private double toEpochMilliScore(java.time.LocalDateTime sentAt) {
        // score를 초 단위가 아니라 ms로 두면 동일 초에 여러 메시지가 와도 ordering 안정성이 좋아짐
        long epochMilli = sentAt.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();
        return (double) epochMilli;
    }

    private record CacheItem(String messageKey, String json, double score) {}

}
