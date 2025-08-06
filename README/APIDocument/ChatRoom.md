# 채팅방 API (ChatController)
채팅방 관리(생성, 조회, 초대 등)은 REST API로, 실시간 메시지 송수신은 WebSocket으로 처리

## 채티방 상태 관리 API
### 채팅방 생성: POST /api/chat/rooms [이동](#채팅방-생성)
- Request: ChatRoomRequest (e.g., { "name": "프로젝트 A팀" })
- Response: ChatRoomResponse (생성된 채팅방 정보)

### 사용자가 참여한 채팅방 목록 조회 GET /api/chat/rooms [이동](#사용자가-참여한-채팅방-목록-조회)
- 인증된 사용자의 정보를 기반으로 참여하고 있는 모든 채팅방 목록을 반환
### 특정 채팅방 정보 조회 GET /api/chat/rooms/{roomId}
- 채팅방 이름, 참여자 목록 등을 반환
### 채팅방에 사용자 초대 POST /api/chat/rooms/{roomId}/user
- Request: { "userId": "초대할 사용자 ID" }

### 채팅방 나가기: DELETE /api/chat/rooms/{roomId}/users/me

### 채팅방 메시지 내역 조회: GET /api/chat/rooms/{roomId}/messages
- 과거 메시지를 페이징(Paging)해서 불러옴 (e.g., ?page=0&size=20)



# WebSocket (ChatController & WebSocketConfig)
실시간으로 메시지를 주고받는 부분. STOMP 프로토콜 사용(pub/sub 모델)

### 메시지 발행 (Publish)
- @MessageMapping("/chat/rooms/{roomId}/messages")
- 클라이언트는 /pub/chat/rooms/{roomId}/messages 목적지로 ChatMessageDto를 전송
### 메시지 구독 (Subscribe)
- 서버는 메시지를 받으면 이 경로를 구독 중인 모든 클라이언트에게 메시지를 브로드캐스팅
- 클라이언트는 /sub/chat/rooms/{roomId}를 구독


# 핵심 기능 흐름 (Flow)

### 메시지 전송 시나리오
1. 클라이언트: 
- WebSocket에 연결합니다.
- 참여하고 있는 채팅방(예: roomId가 123)의 메시지를 받기 위해 /sub/chat/rooms/123을 구독합니다
2. 사용자 A (클라이언트):
  - 채팅 입력창에 "안녕하세요"를 입력하고 '전송' 버튼을 누릅니다.
  - 클라이언트는 /pub/chat/rooms/123/messages 목적지로 아래와 같은 ChatMessageDto 객체를 STOMP를 통해 전송합니다.

 ```
{
  "roomId": 123,
  "senderId": "user_a_id",
  "message": "안녕하세요"
}
```

3. 서버 (ChatController):
- @MessageMapping("/chat/rooms/{roomId}/messages") 어노테이션이 붙은 메소드가 메시지를 수신
- ChatService를 호출하여 받은 메시지를 ChatMessage 엔티티로 변환 후 데이터베이스에 저장
- SimpMessagingTemplate를 사용해서 /sub/chat/room/123 경로로 메시지를 브로드캐스팅

4. 다른 참여자 (클라이언트):
- /sub/chat/rooms/123 을 구독하고 있던 모든 클라이언트(사용자 A포함)가 메시지를 수신하여 화면에 표시


# API 세부사항
### 채팅방 생성

나중에 추가 ㄱㄱ

### 사용자가 참여한 채팅방 목록 조회
사용자가 참여한 각 채팅방에 대해 다음 정보를 리스트 형태로 반환합니다.
- 채팅방 ID('roomID'): 각 채팅방의 고유 식별 ID
- 채팅방 이름('name'): 채팅방 생성 시 부여된 이름 (예: "프로젝트 회의방")
- 참여자 목록('participants'): 해당 채팅방에 참여하고 있는 모든 사용자의 정보 목록. 참여자는 사용자 ID('userID'), 이름('username')을 포함
```
[
  {
    "roomId": "c1b0e2-012e-4b1e-81e0-19nn12c",
    "name": "프로젝트 회의방",
    "participants": [
      {
        "userId": 1,
        "username": "user1"
      },
      {
        "userId": 2,
        "username": "user2"
      }
    ]
  }
]
```
