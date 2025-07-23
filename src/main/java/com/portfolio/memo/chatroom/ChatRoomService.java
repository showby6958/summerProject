package com.portfolio.memo.chatroom;

import com.portfolio.memo.auth.User;
import com.portfolio.memo.auth.UserRepository;
import com.portfolio.memo.chatroom.dto.ChatRoomInfo;
import com.portfolio.memo.chatroom.dto.ChatRoomRequest;
import com.portfolio.memo.chatroom.dto.ChatRoomResponse;
import com.portfolio.memo.chatroom.dto.ParticipantDto;
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
}
