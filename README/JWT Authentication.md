# JWT 인증 핵심 기능 요약
## 1. 초기화(생성자)
application.properties에서 jwt.secret 값을 읽어와 JWT 서명에 사용할 Key 객체를 생성합니다.
이 키는 외부에 노출되면 안 되는 매우 중요한 값입니다.

## 2. 토큰생성('generateToken')
사용자가 성공적으로 로그인하면, Spring Security의 Authentication 객체를 받습니다. 
이 객체에서 사용자 ID와 권한을 추출하여 Access Token과 Refresh Token을 생성하고 JwtToken 객체에 담아 반환합니다.

## 3. 인증 정보 조회('getAuthentication')
API 요청의 헤더에 담겨 온 Access Token을 받아서 토큰을 해석(복호화)합니다. 
토큰 안의 사용자 ID와 권한 정보를 꺼내 Spring Security가 사용할 수 있는 Authentication 객체로 만들어 반환합니다.

## 4. 토큰 검증('validateToken')
토큰이 위변조되지 않았는지, 만료되지는 않았는지 등을 검사합니다.        
JwtAuthenticationFilter에서 매 요청마다 이 메소드를 호출하여 토큰의 유효성을 확인합니다.
