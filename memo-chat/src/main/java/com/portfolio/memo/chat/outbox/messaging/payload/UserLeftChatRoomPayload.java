package com.portfolio.memo.chat.outbox.messaging.payload;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UserLeftChatRoomPayload {

    private Long roomId;
    private Long joinedUserId;

    private List<Long> currentParticipantIds;
}
