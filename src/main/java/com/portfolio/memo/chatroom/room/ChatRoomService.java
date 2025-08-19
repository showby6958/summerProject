package com.portfolio.memo.chatroom.room;

import com.portfolio.memo.auth.User;
import com.portfolio.memo.auth.UserRepository;
import com.portfolio.memo.chatroom.message.dto.ChatMessageHistoryDto;
import com.portfolio.memo.chatroom.message.ChatMessageRepository;
import com.portfolio.memo.chatroom.message.ChatRoomMessage;
import com.portfolio.memo.chatroom.participant.ParticipantDto;
import com.portfolio.memo.chatroom.room.dto.ChatRoomRequest;
import com.portfolio.memo.chatroom.room.dto.ChatRoomResponse;
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

    // 채팅방 초대 api service
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
