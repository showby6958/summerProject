# 채팅방 API (ChatRoomController)
채팅방 관리(생성, 조회, 초대 등)은 REST API로, 실시간 메시지 송수신은 WebSocket으로 처리

## 채팅방 API 목록
### 채팅방 생성: POST /api/chat/rooms [이동](#채팅방-생성)
- 사용자로부터 입력받은 채팅방의 이름으로 채팅방 생성

### 사용자가 참여한 채팅방 목록 조회 GET /api/chat/rooms [이동](#사용자가-참여한-채팅방-목록-조회)
- 인증된 사용자의 정보를 기반으로 참여하고 있는 모든 채팅방 목록을 반환

### 특정 채팅방 정보 조회 GET /api/chat/rooms/{roomId} **이거 아직 없는 기능임(나중에 만드셈)**
- 채팅방 이름, 참여자 목록 등을 반환

### 채팅방에 사용자 초대 POST /api/chat/rooms/{roomId}/invite [이동](#채팅방에-사용자-초대)
- 초대할 유저의 이메일로 채팅방 초대

### 채팅방 나가기: DELETE /api/chat/rooms/{roomId}/users/me **이거 아직 없는 기능임(나중에 만드셈)**

### 채팅방 메시지 내역 조회: GET /api/chat/rooms/{roomId}/messages [이동](#채팅방-메시지-내역-조회)
- 과거 메시지를 작성 시간순 정렬하여 가져옴



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
#### POST http://localhost:8080/api/chat/rooms
사용자가 새로운 채팅방을 생성합니다.
- 채팅방 이름('name'): 채팅방에 부여되는 이름

**요청 형식**<br/>
Headers
```
Content-Type: application/json
Authorization: Bear eyHjQwxJcq...
```

Body
```
{
    "name": "프로젝트 회의방"
}
```

**반환 형식**<br/>
```
{
    "roomId": 8,
    "name": "api 문서 작성 리턴 확인용",
    "participants": [
        "그냥일반유저"
    ],
    "createdAt": "2025-08-01T16:20:49.7063585"
}
```

### 사용자가 참여한 채팅방 목록 조회
#### GET http://localhost:8080/api/chat/rooms
사용자가 참여한 각 채팅방에 대해 다음 정보를 리스트 형태로 반환합니다.
- 채팅방 ID('roomID'): 각 채팅방의 고유 식별 ID
- 채팅방 이름('name'): 채팅방 생성 시 부여된 이름 (예: "프로젝트 회의방")
- 참여자 목록('participants'): 해당 채팅방에 참여하고 있는 모든 사용자의 정보 목록. 참여자는 사용자 ID('userID'), 이름('username')을 포함

**요청 형식**<br/>
Headers
```
Authorization: Bearer eyHJqmj...
```
Body
```

```

**반환 형식**<br/>
Body
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

### 채팅방에 사용자 초대
초대할 사용자의 이메일을 리스트 형태로 반환합니다.
- 이메일('emails'): 초대할 사용자의 이메일(한 번에 다수의 사용자 초대가능)

**요청 형식**<br/>
Headers
```
Authorization: Bearer eyHjbGc...
```
Body
```
{
    "emails": [
        "user1@example.com",
        "user2@example.com"
    ]
}
```

**반환 형식**<br/>
Body
```
```


### 채팅방 메시지 내역 조회
해당 채팅방의 채팅 기록을 조회합니다.

**요청 형식**<br/>
Headers
```
Authorization: Bearer eyHjjqw...
```
Body
```
```

**반환 형식**<br/>
Body
```
[
    {
        "messageId": 1,
        "senderName": "어드민",
        "message": "채팅전송 테스트",
        "sentAt": "2025-07-29T16:05:59.45104"
    },
    {
        "messageId": 4,
        "senderName": "그냥일반유저",
        "message": "테스트 메시지1",
        "sentAt": "2025-08-01T15:05:38.535942"
    },
    {
        "messageId": 5,
        "senderName": "어드민",
        "message": "테스트 메시지2",
        "sentAt": "2025-08-01T15:06:42.839663"
    }
]
```
