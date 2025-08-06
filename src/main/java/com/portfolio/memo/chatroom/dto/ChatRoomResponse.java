package com.portfolio.memo.chatroom.dto;

import com.portfolio.memo.auth.User;
import com.portfolio.memo.chatroom.ChatRoom;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Builder
@Getter
@AllArgsConstructor
public class ChatRoomResponse {

    private Long roomId;
    private String name;
    private List<String> participants;
    private LocalDateTime createdAt;

    // User 객체들을 모두 이름(String)으로 변환한 리스트 생성
    // Set<User>  ->  List<String> 변환
    @Builder
    public static ChatRoomResponse fromEntity(ChatRoom chatRoom) {
        List<String> participantNames = chatRoom.getParticipants().stream()
                .map(User::getName)
                .collect(Collectors.toList());

        return ChatRoomResponse.builder()
                .roomId(chatRoom.getId())
                .name(chatRoom.getName())
                .participants(participantNames)
                .createdAt(chatRoom.getCreatedAt())
                .build();
    }
}
