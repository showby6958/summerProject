package com.portfolio.memo.chat.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.memo.chat.message.dto.SendMessageRequest;
import com.portfolio.memo.chat.message.dto.SendMessageResponse;
import com.portfolio.memo.chat.outbox.domain.OutboxEvent;
import com.portfolio.memo.chat.outbox.domain.OutboxEventRepository;
import com.portfolio.memo.chat.outbox.domain.event.MessageCreatedEvent;
import com.portfolio.memo.chat.participant.ParticipantValidator;
import com.portfolio.memo.common.jwt.CustomUserPrincipal;
import com.portfolio.memo.support.TestPrincipals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @InjectMocks
    private ChatMessageService chatMessageService;

    // --- Mock Dependencies ---
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private OutboxEventRepository outboxEventRepository;
    @Mock
    private ParticipantValidator participantValidator;
    @Mock
    private ChatMessageCacheWriter cacheWriter;
    @Mock
    private StringRedisTemplate chatRedisTemplate; // 사용되진 않지만, 의존성이므로 Mock 처리

    // ObjectMapper는 실제 동작이 필요하므로 @Spy 사용
    @Spy
    private ObjectMapper objectMapper;

    // --- Test Fixtures ---
    private Long roomId;
    private CustomUserPrincipal currentUser;
    private SendMessageRequest sendMessageRequest;

    @BeforeEach
    void setUp() {
        roomId = 1L;
        currentUser = TestPrincipals.user(100L, "testUser");
        sendMessageRequest = new SendMessageRequest();
        sendMessageRequest.setContent("Hello, this is a test message!");
    }

    @Test
    @DisplayName("send 메서드는 메시지를 성공적으로 저장하고 관련 의존성을 올바르게 호출한다")
    void send_shouldSaveMessageAndCallDependenciesCorrectly() throws JsonProcessingException {
        // Given (설정)

        // 1. chatMessageRepository.save()가 호출될 때 반환할 가짜 ChatMessage 객체 생성
        ChatMessage savedMessage = ChatMessage.builder()
                .id(1L)
                .roomId(roomId)
                .senderId(currentUser.getUserId())
                .senderName(currentUser.getUsername())
                .content(sendMessageRequest.getContent())
                .build();
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);

        // 2. participantValidator.validateParticipant()는 아무것도 하지 않도록 설정 (void 메서드)
        doNothing().when(participantValidator).validateParticipant(roomId, currentUser.getUserId());

        // 3. ObjectMapper가 MessageCreatedEvent를 직렬화할 때 사용할 가짜 JSON 생성
        // (실제 직렬화 로직을 테스트하는 것이 아니므로, Spy를 사용했더라도 미리 정의)
        String fakeJsonPayload = "{\"messageId\":1,\"roomId\":1,\"senderId\":100,\"content\":\"...\"}";
        when(objectMapper.writeValueAsString(any(MessageCreatedEvent.class))).thenReturn(fakeJsonPayload);


        // When (실행)
        SendMessageResponse response = chatMessageService.send(roomId, sendMessageRequest, currentUser);


        // Then (검증)

        // 1. 반환된 응답 DTO 검증
        assertThat(response).isNotNull();
        assertThat(response.getMessageId()).isEqualTo(savedMessage.getId());
        assertThat(response.getSenderName()).isEqualTo(currentUser.getUsername());
        assertThat(response.getContent()).isEqualTo(sendMessageRequest.getContent());

        // 2. participantValidator가 올바른 인자로 호출되었는지 검증
        verify(participantValidator, times(1)).validateParticipant(roomId, currentUser.getUserId());

        // 3. chatMessageRepository.save()에 전달된 ChatMessage 객체의 내용을 검증
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository, times(1)).save(messageCaptor.capture());
        ChatMessage capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage.getRoomId()).isEqualTo(roomId);
        assertThat(capturedMessage.getSenderId()).isEqualTo(currentUser.getUserId());
        assertThat(capturedMessage.getContent()).isEqualTo(sendMessageRequest.getContent());

        // 4. outboxEventRepository.save()에 전달된 OutboxEvent 객체의 내용을 검증
        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository, times(1)).save(outboxCaptor.capture());
        OutboxEvent capturedOutboxEvent = outboxCaptor.getValue();
        assertThat(capturedOutboxEvent.getAggregateType()).isEqualTo("ChatMessage");
        assertThat(capturedOutboxEvent.getAggregateId()).isEqualTo(savedMessage.getId());
        assertThat(capturedOutboxEvent.getPayload()).isEqualTo(fakeJsonPayload);

        // 5. cacheWriter가 올바른 인자(저장된 메시지)로 호출되었는지 검증
        verify(cacheWriter, times(1)).cache(savedMessage);
    }
}