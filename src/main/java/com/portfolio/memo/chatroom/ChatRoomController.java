package com.portfolio.memo.chatroom;

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

    @PostMapping("/rooms")
    public ResponseEntity<ChatRoomResponse> createRoom(
            @RequestBody ChatRoomRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        ChatRoomResponse response = chatRoomService.createChatRoom(request, userDetails.getUsername());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomInfo>> getUserChatRooms(@AuthenticationPrincipal UserDetails userDetails) {
        List<ChatRoomInfo> chatRooms = chatRoomService.getUserChatRooms(userDetails.getUsername());
        return ResponseEntity.ok(chatRooms);
    }
}
