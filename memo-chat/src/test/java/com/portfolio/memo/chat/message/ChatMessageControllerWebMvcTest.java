package com.portfolio.memo.chat.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.memo.chat.message.dto.SendMessageRequest;
import com.portfolio.memo.chat.message.dto.SendMessageResponse;
import com.portfolio.memo.common.jwt.CustomUserPrincipal;
import com.portfolio.memo.common.jwt.JwtAuthenticationFilter;
import com.portfolio.memo.support.TestPrincipals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatMessageController.class)
@AutoConfigureMockMvc(addFilters = false)
public class ChatMessageControllerWebMvcTest {
    // 목표: HTTP 바인딩 + 서비스 호출 파라미터 검증

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    ChatMessageService chatMessageService;
    
    @MockitoBean
    SimpMessageSendingOperations simpMessageSendingOperations;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private CustomUserPrincipal currentUser;

    // @AuthenticationPrincipal 주입을 위해 SecurityContext에 principal을 심는 방식을 사용
    @BeforeEach
    void setUp() {
        currentUser = TestPrincipals.user(2L, "jang");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUser, null, currentUser.getAuthorities())
        );
    }

    @Test
    void sendMessage_정상요청이면_200_그리고_service호출파라미터가_정확하다() throws Exception {
        // given-when-then 패턴
        // given 단계(테스트에 필요한 모든 것을 설정)
        // 컨트롤러 메서드 파라미터 3개 (roomId, request, currentUser)
        Long roomId = 1L; // roomId 설정

        SendMessageRequest req = new SendMessageRequest(); // request 설정
        req.setContent("hello Test!");

        SendMessageResponse fakeResponse = new SendMessageResponse(); // 가짜 응답 객체 생성(서비스 로직을 실행하지 않고도 컨트롤러가 정상적으로 작동하도록)

        when(chatMessageService.send(eq(roomId), any(SendMessageRequest.class), eq(currentUser)))
                .thenReturn(fakeResponse); // 응답으로 SendMessageResponse를 기대

        ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);

        // when(테스트 동작 수행)
        mockMvc.perform(post("/api/chat/rooms/{roomId}/messages", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk()); // HTTP 상태코드 검증


        // then(실행결과 검증) 단계
        // Service 호출 검증 - 서비스가 정확한 파라미터로 호출되었는지 검증
        // 1. JSON -> DTO로 정상 파싱되는지,
        // 2. 그 DTO가 service로 넘어갈 때, content 값이 유지되는지 검증
        verify(chatMessageService).send(eq(roomId), captor.capture(), eq(currentUser));
        assertThat(captor.getValue().getContent()).isEqualTo("hello Test!");
    }


}
