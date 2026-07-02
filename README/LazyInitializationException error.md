# LazyInitializationException 지연로딩예외
ChatRoomMessage 엔티티 내부에 sender라는 필드를 가지고 있습니다. 이 필드는 User 엔티티와 연결되어 있는 상황입니다.
```
(ChatRoomMessage.java)
@ManyToOne(fetch = FetchType.LAZY) <-- 이 부분
private Uer sender;
```

## 1. 문제점(이전 코드의 동작 방식)
1. API 요청: 클라이언트가 /api/chat/rooms/{roomId}/messages를 호출합니다.
2. 데이터 조회: ChatRoomService는 데이터베이스에서 ChatRoomMessage 목록을 가져옵니다. 이때 '지연로딩' 설정 때문에, 각 ChatRoomMessage 객체의 sender 필드는 실제 User 데이터가 아닌, 가짜 프록시(Proxy) 객체로 채워져 있습니다.
3. DB 세션 종료: ChatRoomService의 getChatHistory 메소드가 끝나면, 데이터베이스와의 연결(세션)이 닫힙니다.
4. JSON 변환 시도: 이제 Spring은 컨트롤러가 받은 ChatRoomMessage 목록을 JSON으로 변환해서 클라이언트에게 보내려고 합니다.
5. 예외 발생: JSON 변환기는 sender 필드를 보고, 그 안의 name 같은 정보를 읽으려고 sender 객체에 접근합니다. 이때 가짜 프록시 객체는 "이제 진짜 데이터가 필요하구나" 하고 뒤늦게 데이터베이스에서 User정보를 가져오려고 시도합니다
6. 하지만 이미 3번 단계에서 데이터베이스 연결이 끊긴 상태이기 때문에, LazyInitializationException이라는 예외가 발생합니다.

## 2. 해결방법(현재 코드의 동작 방식)
1. ChatMessageHistoryDto 생성: senderName, message 등 클라이언트에게 꼭 필요한 데이터만 담을 DTO를 만듭니다.
2. 서비스 계층에서 변환: ChatRoomService 안에서 ChatRoomMessage 엔티티 목록을 조회한 직후, 데이터베이스 연결이 살아있을 때 각 엔티티를 ChatMessageHistoryDto로 변환을 합니다.
이 시점에서는 DB연결이 유효하므로, 지연 로딩이 정상적으로 동작하여 User 정보를 가져와 DTO의 senderName 필드에 채워 넣습니다.
3. 컨트롤러는 DTO를 반환: 컨트롤러는 이제 엔티티가 아닌, 모든 정보가 채워진 새 DTO 목록을 JSON으로 변환하여 클라이언트에게 보냅니다. 이 DTO는 더 이상 데이터베이스에 접근할 일이 없으므로 아무런 예외도 발생하지 않습니다.


## 요약
- 문제: DB 연결이 끊긴 후에 지연 로딩된 데이터(sender)에 접근하려다 오류 발생
- 해결: DB 연결이 살아있는 동안 미리 필요한 모든 데이터를 DTO에 담아두고, DB와 상관없는 DTO를 클라이언트에 전달하여 문제를 해결

## 참고
DTO를 사용하는 대신 fetch = FetchType.EAGER를 사용한다면 LazyInitializationException 문제를 해결 할 수 있습니다.
<br />
그렇다면 JPA는 ChatRoomMessage를 조회할 때 항상 연관된 User 엔티티까지 함께 조회해서 sender 필드를 채워 넣습니다.

하지만 EAGER를 사용한다면 여러 문제가 추가로 발생할 수 있습니다.
1. 성능 문제(N+1 문제): 만약 채팅 메시지 100개를 조회한다고 가정한다면, ChatRoomMessage를 가져오는 쿼리 1번이 실행됩니다. 그 후, 각 메시지에 연결된 sender를 가져오기 위해 최악의 경우100번의 추가 쿼리가 발생 할 수 있습니다. (총 1 + 100 번의 쿼리)
2. 불필요한 데이터 로딩: 단순히 메시지 내용만 필요한 경우에도 EAGER 설정 때문에 항상 User 정보까지 함께 조회하게 되기 때문에 불필요한 데이터베이스 부하와 메모리 사용을 유발합니다.
3. 예측 불가능성: 엔티티의 관계가 복잡해질수록 EAGER로딩이 연쇄적으로 일어나면서 의도하지 않은 수 많은 쿼리가 발생할 수 있습니다.
