package com.portfolio.memo.chat.room;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.memo.chat.room.dto.ChatRoomRequest;
import com.portfolio.memo.chat.room.dto.ChatRoomResponse;
import com.portfolio.memo.common.jwt.CustomUserPrincipal;
import com.portfolio.memo.common.jwt.JwtAuthenticationFilter;
import com.portfolio.memo.support.TestPrincipals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatRoomController.class)
@AutoConfigureMockMvc(addFilters = false) // Spring Security 필터 비활성화
class ChatRoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ChatRoomService chatRoomService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter; // SecurityConfig 의존성 Mock 처리

    private CustomUserPrincipal currentUser;

    @BeforeEach
    void setUp() {
        // 테스트용 Principal 설정
        currentUser = TestPrincipals.user(100L, "testUser");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUser, null, currentUser.getAuthorities())
        );
    }

    @Test
    @DisplayName("채팅방 생성 요청 시 201 CREATED 상태와 생성된 방 정보를 반환해야 한다")
    void createRoom_shouldReturn201AndRoomInfo() throws Exception {
        // Given (설정)
        List<Long> participantIds = List.of(200L, 300L);
        ChatRoomRequest requestDto = new ChatRoomRequest();
        requestDto.setRoomName("Test Room");
        requestDto.setParticipantUserIds(participantIds);

        ChatRoomResponse responseDto = ChatRoomResponse.builder()
                .roomId(1L)
                .name("Test Room")
                .build();

        // chatRoomService.createChatRoom 메서드가 호출될 때의 행동 정의
        when(chatRoomService.createChatRoom(
                eq("Test Room"),
                eq(currentUser.getUserId()),
                eq(currentUser.getUsername()),
                eq(participantIds)
        )).thenReturn(responseDto);

        // When & Then (실행 및 검증)
        mockMvc.perform(post("/api/chat/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated()) // 201 CREATED 상태 코드 확인
                .andExpect(header().string("Location", "/api/chat/rooms/" + responseDto.getRoomId())) // Location 헤더 확인
                .andExpect(jsonPath("$.roomId").value(responseDto.getRoomId())) // 응답 본문 JSON 필드 확인
                .andExpect(jsonPath("$.name").value(responseDto.getName()));

        // 서비스 메서드가 올바른 인자로 호출되었는지 검증
        verify(chatRoomService).createChatRoom(
                eq("Test Room"),
                eq(currentUser.getUserId()),
                eq(currentUser.getUsername()),
                eq(participantIds)
        );
    }
}
