package com.portfolio.memo.chat.room;

import com.portfolio.memo.chat.participant.InviteRequest;
import com.portfolio.memo.chat.room.dto.ChatHistoryMessageDto;
import com.portfolio.memo.chat.room.dto.ChatRoomRequest;
import com.portfolio.memo.chat.room.dto.ChatRoomResponse;
import com.portfolio.memo.common.jwt.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;


    // 채팅방 생성 (방 생성 + 생성자를 참가자로 자동 등록)
    @PostMapping("/rooms")
    public ResponseEntity<ChatRoomResponse> createRoom(
            @RequestBody ChatRoomRequest request,
            @AuthenticationPrincipal CustomUserPrincipal currentUser) {

        ChatRoomResponse response = chatRoomService.createChatRoom(
                request.getRoomName(),
                currentUser.getUserId(),
                currentUser.getUsername(),
                request.getParticipantUserIds()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .location(URI.create("/api/chat/rooms/" + response.getRoomId()))
                .body(response);
    }

//    // 참여하고 있는 채팅방 정보를 조회하는 api
//    @GetMapping("/rooms")
//    public ResponseEntity<List<ChatRoomInfo>> getUserChatRooms(
//            @AuthenticationPrincipal CustomUserPrincipal currentUser) {
//
//        List<ChatRoomInfo> chatRooms = chatRoomService.getChatRoomInfo(currentUser.getUserId());
//        return ResponseEntity.ok(chatRooms);
//    }

    // 채팅 기록을 불러오는 api
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<List<ChatHistoryMessageDto>> getChatHistory(
            @PathVariable Long roomId,
            @AuthenticationPrincipal CustomUserPrincipal currentUser) {

        List<ChatHistoryMessageDto> chatHistory = chatRoomService.getChatHistory(roomId, currentUser.getUserId());

        return ResponseEntity.ok(chatHistory);
    }


    // 채팅방에 유저를 초대하는 api
    @PostMapping("/rooms/{roomId}/invite")
    public ResponseEntity<Void> inviteUsers(
            @PathVariable Long roomId,
            @RequestBody InviteRequest inviteRequest,
            @AuthenticationPrincipal CustomUserPrincipal currentUser) {

        chatRoomService.inviteUsersToChatRoom(
                roomId,
                inviteRequest.getUserIds(),
                currentUser.getUserId()
        );

        return ResponseEntity.ok().build();
    }
}
