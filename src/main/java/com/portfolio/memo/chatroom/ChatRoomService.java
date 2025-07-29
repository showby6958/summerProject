package com.portfolio.memo.chatroom;

import com.portfolio.memo.auth.User;
import com.portfolio.memo.auth.UserRepository;
import com.portfolio.memo.chatroom.dto.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;

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

        return chatMessageRepository.save(chatMessage);
    }


    // 채팅 기록(생성시간을 오름차순으로 조회)을 불러오는 api service
    public List<ChatMessageHistoryDto> getChatHistory(Long roomId) {
        List<ChatRoomMessage> messages = chatMessageRepository.findByChatRoom_IdOrderBySentAtAsc(roomId);
        return messages.stream()
                .map(ChatMessageHistoryDto::fromEntity)
                .collect(Collectors.toList());
    }
}
