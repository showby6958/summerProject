package com.portfolio.memo.chat.room.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomRequest {
    private String roomName;
    private List<Long> participantUserIds; // 방 생성 시점에 초대할 사용자 ID 목록
}
