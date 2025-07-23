package com.portfolio.memo.chatroom.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class ChatRoomInfo {
    private final Long roomId;
    private final String name;
    private final List<ParticipantDto> participantDto;

}
