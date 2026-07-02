package com.portfolio.memo.chat.participant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParticipantValidatorTest {

    @Mock
    private ChatRoomParticipantRepository participantRepository;

    @InjectMocks
    private ParticipantValidator participantValidator;

    private Long testRoomId;
    private Long testUserId;

    @BeforeEach
    void setUp() {
        testRoomId = 1L;
        testUserId = 100L;
    }

    @Test
    @DisplayName("유효한 참여자인 경우 예외가 발생하지 않아야 한다")
    void validateParticipant_validParticipant_noExceptionThrown() {
        // Given
        when(participantRepository.existsByRoomIdAndUserId(testRoomId, testUserId)).thenReturn(true);

        // When & Then
        assertDoesNotThrow(() -> participantValidator.validateParticipant(testRoomId, testUserId));
        verify(participantRepository, times(1)).existsByRoomIdAndUserId(testRoomId, testUserId);
    }

    @Test
    @DisplayName("roomId가 null인 경우 IllegalArgumentException이 발생해야 한다")
    void validateParticipant_nullRoomId_throwsIllegalArgumentException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> participantValidator.validateParticipant(null, testUserId));
        assertEquals("roomId and userId must not be null", exception.getMessage());
        verifyNoInteractions(participantRepository); // 리포지토리는 호출되지 않아야 함
    }

    @Test
    @DisplayName("userId가 null인 경우 IllegalArgumentException이 발생해야 한다")
    void validateParticipant_nullUserId_throwsIllegalArgumentException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> participantValidator.validateParticipant(testRoomId, null));
        assertEquals("roomId and userId must not be null", exception.getMessage());
        verifyNoInteractions(participantRepository); // 리포지토리는 호출되지 않아야 함
    }

    @Test
    @DisplayName("참여자가 아닌 경우 AccessDeniedException이 발생해야 한다")
    void validateParticipant_notParticipant_throwsAccessDeniedException() {
        // Given
        when(participantRepository.existsByRoomIdAndUserId(testRoomId, testUserId)).thenReturn(false);

        // When & Then
        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> participantValidator.validateParticipant(testRoomId, testUserId));
        assertTrue(exception.getMessage().contains("User is not a participant of the room"));
        verify(participantRepository, times(1)).existsByRoomIdAndUserId(testRoomId, testUserId);
    }
}
