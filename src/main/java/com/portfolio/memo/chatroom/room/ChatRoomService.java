package com.portfolio.memo.chatroom.room;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.memo.RedisSerializer;
import com.portfolio.memo.auth.CustomUserDetails;
import com.portfolio.memo.auth.User;
import com.portfolio.memo.auth.UserRepository;
import com.portfolio.memo.chat.dto.ChatMessage;
import com.portfolio.memo.chatroom.message.MessageDeleteNotAllowed;
import com.portfolio.memo.chatroom.message.dto.ChatMessageDto;
import com.portfolio.memo.chatroom.message.dto.ChatMessageHistoryDto;
import com.portfolio.memo.chatroom.message.ChatMessageRepository;
import com.portfolio.memo.chatroom.message.ChatRoomMessage;
import com.portfolio.memo.chatroom.participant.ParticipantDto;
import com.portfolio.memo.chatroom.room.dto.ChatRoomRequest;
import com.portfolio.memo.chatroom.room.dto.ChatRoomResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final RedisTemplate<String, String> redisTemplate;
    // ObjectMapper: Java 객체 <-> JSON, 직렬화 or 역직렬화에 사용
//    private final ObjectMapper objectMapper;

    private final RedisSerializer redisSerializer;

    private static final long CACHE_TTL_HOURS = 12; // 캐시 만료 시간

    // 새 채팅방 생성
    @Transactional
    public ChatRoomResponse createChatRoom(ChatRoomRequest request, String creatorEmail) {
        User creator = userRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다: " + creatorEmail));


        ChatRoom newRoom = ChatRoom.builder()
                .name(request.getName())
                .build();
        newRoom.getParticipants().add(creator); // 생성자를 참여자에 추가

        ChatRoom saveRoom = chatRoomRepository.save(newRoom);

        return ChatRoomResponse.fromEntity(saveRoom);
    }

    public List<ChatRoomInfo> getUserChatRooms(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        List<ChatRoom> chatRooms = chatRoomRepository.findByParticipantsContaining(user);

        return chatRooms.stream()
                .map(this::convertToChatRoomInfo)
                .collect(Collectors.toList());
    }

    private ChatRoomInfo convertToChatRoomInfo(ChatRoom chatRoom) {
        List<ParticipantDto> participantDtos = chatRoom.getParticipants().stream()
                .map(user -> ParticipantDto.builder()
                        .userId(user.getId())
                        .username(user.getName())
                        .build())
                .collect(Collectors.toList());

        return ChatRoomInfo.builder()
                .roomId(chatRoom.getId())
                .name(chatRoom.getName())
                .participantDto(participantDtos)
                .build();
    }



    /*
    // 채팅 기록(생성시간을 오름차순으로 조회)을 불러오는 api
    public List<ChatMessageHistoryDto> getChatHistory(Long roomId, String userEmail) {
        // 1. 채팅방과 사용자가 존재하는지 확인
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("채팅방을 찾을 수 없습니다: " + roomId));
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다: " + userEmail));

        // 2. 요청한 사용자가 해당 채팅방의 참여자인지 권한 확인
        boolean isParticipant = chatRoom.getParticipants().stream()
                .anyMatch(participant -> participant.getId().equals(user.getId()));

        if (!isParticipant) {
            throw new SecurityException("이 채팅방의 기록을 볼 권한이 없습니다.");
        }

        // 3. 권한이 확인되면, 채팅 기록을 반환
        List<ChatRoomMessage> messages = chatMessageRepository.findByChatRoom_IdOrderBySentAtAsc(roomId);
        return messages.stream()
                .map(ChatMessageHistoryDto::fromEntity)
                .collect(Collectors.toList());
    }
    */
    // 채팅 기록을 불러오는 api (Redis 캐시 조회)
    public List<ChatMessageHistoryDto> getChatHistory(Long roomId, String userEmail) {
        // 1. 사용자 및 채팅방 참여 권한 확인
        validateRoomParticipant(roomId, userEmail);

        String redisKey = "chat:room:" + roomId + ":messages";
        ZSetOperations<String, String> zSetOperations = redisTemplate.opsForZSet();

        // 2. Redis 캐시에서 데이터 조회 (Cache-Aside)
        Set<String> messageJsonSet = zSetOperations.range(redisKey, 0, -1);

        // 3. Cache Hit: Redis에 데이터가 있는 경우,
        // Redis 캐시에 메시지가 있으면 DB를 조회하지 않고 바로 리스트 형태로 반환
        if (messageJsonSet != null && !messageJsonSet.isEmpty()) {
            // 어떤 방에서 Cache Hit 발생했는지 기록
            log.info("Cache Hit for chat room: {}", roomId);
            return messageJsonSet.stream()
                    .map(json -> redisSerializer.deserialize(json, ChatMessageHistoryDto.class))
                    .collect(Collectors.toList()); // 결과를 List<ChatMessageHistoryDto>로 모음
        }

        // 4. Cache Miss: Redis에 데이터가 없는 경우, DB에서 조회
        // 어떤 방에서 Cache Miss 발생했는지 기록
        log.info("Cache Miss for chat room: {}", roomId);
        List<ChatRoomMessage> messagesFromDb = chatMessageRepository.findByChatRoomIdAndIsDeletedFalseOrderBySentAtAsc(roomId);
        // DB에서 가져온 메시지를 클라이언트가 사용할 ChatMessageHistoryDto로 변환
        List<ChatMessageHistoryDto> chatHistory = messagesFromDb.stream()
                .map(ChatMessageHistoryDto::fromEntity)
                .collect(Collectors.toList());

        // 5. DB에서 가져온 데이터로 Redis 캐시 채우기
        for (ChatMessageHistoryDto dto : chatHistory) {
            // DTO 객체 -> JSON 문자열로 변환
            String json = redisSerializer.serialize(dto);

            // 메시지 ID 순(오래된 메시지부터)으로 ZSet에 저장됨
            zSetOperations.add(redisKey, json, dto.getMessageId());
        }
        // 캐시 만료 시간 설정
        redisTemplate.expire(redisKey, CACHE_TTL_HOURS, TimeUnit.HOURS);

        return chatHistory;
    }

    // 채팅방 참여 권한 확인
    private void validateRoomParticipant(Long roomId, String userEmail) {
        // 1. 채팅방과 사용자가 존재하는지 확인
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("채팅방을 찾을 수 없습니다: " + roomId));
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다: " + userEmail));

        // 2. 요청한 사용자가 해당 채팅방의 참여자인지 권한 확인
        boolean isParticipant = chatRoom.getParticipants().stream()
                .anyMatch(participant -> participant.getId().equals(user.getId()));

        if (!isParticipant) {
            throw new SecurityException("이 채팅방의 기록을 볼 권한이 없습니다.");
        }
    }


    // 채팅방 초대 api
    @Transactional
    public void inviteUsersToChatRoom(Long roomId, List<String> userEmails, String inviterEmail) {
        // 1. roomId로 채팅방이 있는지 조회
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("채팅방을 찾을 수 없습니다: " + roomId));

        // 2. inviterEmail로 초대한 사용잦가 해당 채팅방에 참여하고 있는지 확인(권한 확인)
        User user = userRepository.findByEmail(inviterEmail)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다: " + inviterEmail));

        // 3. userEmails 목록으로 User 목록을 조회(초대하는 사용자가 존재하는지 체크)
        List<User> userToInvite = userRepository.findAllByEmailIn(userEmails);
        if (userToInvite.size() != userEmails.size()) {
            throw new EntityNotFoundException("일부 사용자를 찾을 수 없습니다.");
        }

        // 4. 조회된 User들을 ChatRoom의 participants에 추가
        chatRoom.getParticipants().addAll(userToInvite);

        // 5. 변경된 ChatRoom 저장
        chatRoomRepository.save(chatRoom);
    }

}
