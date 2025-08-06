package com.portfolio.memo.chatroom;

import com.portfolio.memo.chatroom.dto.ChatMessageHistoryDto;
import com.portfolio.memo.chatroom.dto.ChatRoomInfo;
import com.portfolio.memo.chatroom.dto.ChatRoomRequest;
import com.portfolio.memo.chatroom.dto.ChatRoomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    // 채팅방 생성하는 api(유저로 부터 채팅방 이름을 입력 받음)
    @PostMapping("/rooms")
    public ResponseEntity<ChatRoomResponse> createRoom(
            @RequestBody ChatRoomRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        ChatRoomResponse response = chatRoomService.createChatRoom(request, userDetails.getUsername());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 참여하고 있는 채팅방 정보를 조회하는 api
    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomInfo>> getUserChatRooms(@AuthenticationPrincipal UserDetails userDetails) {
        List<ChatRoomInfo> chatRooms = chatRoomService.getUserChatRooms(userDetails.getUsername());
        return ResponseEntity.ok(chatRooms);
    }

    // 채팅 기록을 불러오는 api
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<List<ChatMessageHistoryDto>> getChatHistory(@PathVariable Long roomId) {

        List<ChatMessageHistoryDto> chatHistory = chatRoomService.getChatHistory(roomId);
        return ResponseEntity.ok(chatHistory);
    }
}
