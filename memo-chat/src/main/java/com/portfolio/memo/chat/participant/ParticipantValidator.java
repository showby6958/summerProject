package com.portfolio.memo.chat.participant;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;



@Component
@RequiredArgsConstructor
public class ParticipantValidator {

    private final ChatRoomParticipantRepository participantRepository;

    public void validateParticipant(Long roomId, Long userId) {
        if (roomId == null || userId == null) {
            throw new IllegalArgumentException("roomId and userId must not be null");
        }

        boolean isParticipant = participantRepository.existsByRoomIdAndUserId(roomId, userId);
        if (!isParticipant) {
            throw new AccessDeniedException("User is not a participant of the room. roomId=" + roomId + ", userId=" + userId);
        }
    }
}
