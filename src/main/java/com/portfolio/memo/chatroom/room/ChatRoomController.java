package com.portfolio.memo.chatroom.room;

import com.portfolio.memo.chatroom.message.dto.ChatMessageHistoryDto;
import com.portfolio.memo.chatroom.participant.InviteRequest;
import com.portfolio.memo.chatroom.room.dto.ChatRoomRequest;
import com.portfolio.memo.chatroom.room.dto.ChatRoomResponse;
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
    public ResponseEntity<List<ChatMessageHistoryDto>> getChatHistory(@PathVariable Long roomId, @AuthenticationPrincipal UserDetails userDetails) {

        List<ChatMessageHistoryDto> chatHistory = chatRoomService.getChatHistory(roomId, userDetails.getUsername());
        return ResponseEntity.ok(chatHistory);
    }


    // 채팅방에 유저를 초대하는 api
    @PostMapping("/rooms/{roomId}/invite")
    public ResponseEntity<Void> inviteUsers(
            @PathVariable Long roomId,
            @RequestBody InviteRequest inviteRequest,
            @AuthenticationPrincipal UserDetails userDetails
            ) {
        chatRoomService.inviteUsersToChatRoom(
                roomId,
                inviteRequest.getEmails(),
                userDetails.getUsername()
        );

        return ResponseEntity.ok().build();
    }
}
