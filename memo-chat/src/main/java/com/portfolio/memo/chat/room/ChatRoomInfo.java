package com.portfolio.memo.chat.room;

import com.portfolio.memo.chat.participant.ParticipantDto;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ChatRoomInfo {
    private final Long roomId;
    private final String name;
    private final List<ParticipantDto> participantDto;
    private final LocalDateTime createdAt;

}
