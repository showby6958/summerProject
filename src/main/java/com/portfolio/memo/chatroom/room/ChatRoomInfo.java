package com.portfolio.memo.chatroom.room;

import com.portfolio.memo.chatroom.participant.ParticipantDto;
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
