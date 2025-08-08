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

### 메시지 읽음 확인 표시 SEND /api/chat.readMessage [이동](#메시지-읽음-확인)
- 각 사용자가 해당 메시지를 읽었는지 확인하는 기능



# WebSocket (StompHandler & WebSocketConfig)
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

## 메시지 읽음 확인 시나리오
참여자: 채팅방에 A, B, C 세 명의 사용자가 있습니다. <br/>
상황: 
1. 사용자 A가 "안녕하세요"라는 메시지를 보냅니다
2. 잠시 후, 사용자 B가 채팅방에 들어와서 그 메시지를 읽습니다.
3. 사용자 C는 아직 메시지를 읽지 않았습니다.

### - 단계 1: 메시지 전송 및 초기 상태
1. 송신 (Client &rarr; Server): 사용자 A가 메시지를 보내면, 클라이언트는 /chat.sendMessage(기존의 메시지 전송 경로) 경로로 메시지 내용을 전송
2. 저장 (Server): ChatMessageController의 sendMessage 메서드가 이를 받아 ChatRoomService를 통해 메시지를 chat_messages 테이블에 저장합니다.
3. 초기 상태: 이때 "안녕하세요" 메시지는 아무도 읽지 않은 상태입니다. 이 메시지를 읽어야할 사람은 B와 C 두 명입니다. 따라서 이 메시지의 '안 읽은 사람 수(unreadCount)' 는 2가 됩니다.
4. 브로드캐스트 (Server &rarr; All Clients): 서버는 이 메시지 정보를 /topic/chat/rooms/{roomId} 토픽으로 브로드캐스팅합니다. 클라이언트는 이 메시지를 받아 화면에 표시하며, 메시지 옆에 안 읽음 카운트 '2'를 표시합니다.

### - 단계 2: 메시지 읽음 처리
#### 1. 읽음 감지 (Client - 사용자 B)
사용자 B의 클라이언트(브라우저)는 Intersection Observer API 등을 사용해 "안녕하세요" 메시지가 화면에 보이는 것을 감지합니다. ReadMessageRequest DTO에 읽은 메시지의 messageId와 roomId를 담아 WebSocket을 통해 /chat.readMessage 경로로 메시지를 전송합니다.

#### 2. 읽음 요청 수신 (Server - `ChatMessageController`)
@MessageMapping("/chat.readMessage") 어노테이션이 붙은 readMessage 메서드가 이 요청을 받습니다. (Authentication 객체에서 현재 요청을 보낸 사용자가 B인것을 확인), 컨트롤러는 messageId와 사용자 B의 userEmail 정보를 ChatMessageService의 markAsRead 메서드로 넘겨줍니다.

#### 3. 핵심 로직 처리 (Server - `ChatMessageService`의 `markAsRead` 메서드)
- a. 데이터 조회: messageId로 ChatRoomMessage("안녕하세요" 메시지)엔티티를, userEmail로 User(사용자 B) 엔티티를 DB에서 조회
- b. 중복 확인: ChatMessageReadStatusRepository의 existsByChatMessageAndUser 메서드를 호출하여, 사용자 B가 이 메시지를 이미 읽었는지 확인(중복 저장 방지).
- c. 읽음 상태 저장: 아직 읽지 않았다면 new ChatMessageReadStatus(message, user)를 통해 새로운 읽음 상태 객체를 생서아혹 DB의 chat_message_read_status 테이블에 저장 
- d. unreadCount 재계산: 채팅방의 총 참여자 수(totalParticipants)를 가져옵니다, readStatusRepository.countByChatMessage(message)를 호출하여 "안녕하세요" 메시지를 읽은 사람의 수를 계산(A와 B, 2명), unreadCount = (총 참여자 3명) - (읽은 사람 수 2명) = 1
- e. 결과 반환: 계산된 unreadCount 값(1)과 messageId, roomId를 ChatMessageDto에 담아 ChatMessageController로 반환합니다.

### - 단계 3: 모든 클라이언트 UI 업데이트
1. 업데이트 수신 (All Clients - A, B, C)<br/>
채팅방에 있는 모든 사용자는 /topic/chat/rooms/{roomId}/read 토픽을 구독하고 있다가, unreadCount가 1로 변경된 ChatMessageDto를 수신합니다.
2. UI 업데이트 (Client - A, B, C)<br/>
클라이언트의 JavaScript 코드는 수신된 DTO에서 messageId 를 확인합니다.<br/>
화면에서 해당 messageId를 가진 "안녕하세요" 메시지를 찾습니다.<br/>
메시지 옆에 "안 읽음 카운트"를 '2'에서 DTO에 담겨온 unreadCount 값인 '1'로 변경합니다.


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

### 메시지 읽음 확인
읽음 정보 저장을 위한 별도 테이블 ChatMessageReadStatus 생성
실시간 업데이트를 위한 WebSocket, 사용자가 메시지를 읽었을 때, 클라이언트가 서버로 '읽음' 신호를 WebSocket으로 전송
서버는 읽음 신호를 받으면 DB를 업데이하고, 해당 채팅방에 다른 사옹자에게 읽음 카운트가 변경되었음을 알림
