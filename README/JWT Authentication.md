# JwtTokenProvider 핵심 흐름 요약
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

# JwtAuthenticationFilter 핵심 흐름 요약
## 1. 요청 가로채기
클라이언트로부터 API 요청이 들어오면 doFilter 메소드가 가장 먼저 실행됩니다.

## 2. 토큰 추출('resolveToken')
요청의 Authorization 헤더에서 Bearer 접두사를 제거하고 순수한 JWT 문자열을 얻습니다

## 3. 토큰 유요성 검증
JwtTokenProvider의 validateToken 메소드를 호출하여 토큰이 위변조되지 않았는지, 만료되지 않았는지 등을 검사합니다.

## 4. 인증 정보 설정
토큰이 유효하면, JwtTokenProvider의 getAuthentication 메소드를 통해 토큰에 담긴 사용자 정보를 기반으로 Authentication 객체를 생성합니다.

## 5. SecurityContext에 저장
생성된 Authentication 객체를 SecurityContextHolder에 저장합니다.
이렇게 하면 Spring Security는 "현재 이 요청은 인증된 사용자의 요청이다"라고 인지하게 됩니다.
이 시점부터 컨트롤러 등에서 @AuthenticationPrincipal 어노테이션을 사용하여 로그인된 사용자 정보를 직접 주입받을 수 있습니다.

## 6. 다음 필터로 전달
현재 필터의 작업이 끝나면 chain.doFilter()를 호출하여 요청을 다음 필터로 넘겨줍니다. 
만약 유효하지 않은 토큰이라면 인증 정보 설정 과정 없이 바로 다음 필터로 넘어가게 되므로, 해당 요청은 '미인증' 상태로 처리됩니다.

##  
이 필터는 SecurityConfig에서 http.addFilterBefore(...) 설정을 통해 Spring Security의 필터 체인 중 UsernamePasswordAuthenticationFilter (일반적인 폼 로그인 처리 필터) 보다 먼저 실행되도록 등록되었습니다. 따라서 모든 요청에 대해 JWT 인증을 먼저 시도하게 됩니다.

# 
1. 왜 일반적인 폼 로그인 처리필터(UsernamePasswordAuthenticaiorFilter)보다 먼저 실행되도록 등록 했을까?

이유는 인증 처리 방식의 우선순위 떄문입니다.

- UsernamePasswordAuthenticaiorFilter: 이 필터는 전통적인 방식의 로그인, 즉 사용자가 ID와 비밀번호를 HTML폼에 입력하여 제출했을 때
동작합니다. 주로 웹 페이지의 로그인 화면에서 사용됩니다.
- JwtAuthenticationFilter: 이 필터는 ID/비밀번호가 아닌, 이미 발급된 JWT 토큰을 기반으로 인증을 처리합니다. 로그인 이후의 모든 API 요청 에서 사용됩니다.

클라이언트(예: 웹 브라우저, 모바일 앱)는 로그인에 성공한 후 서버로부터 JWT를 발급받습니다. 그리고 그 이후의 모든 요청(예: "내 정보 조회", "게시글 작성")에는 ID/비밀번호 대신 이 JWT를 Authorization 헤더에 담아 보냅니다.

따라서 서버 입장에서는 로그인 요청이 아닌 일반적인 API 요청이 들어왔을 때, ID/비밀번호를 확인하는 UsernamePasswordAuthenticaiorFilter 보다 JWT가 있는지 먼저 확인하는 JwtAuthenticationFilter를 앞에 두는 것이 훨씬 효율적입니다.

만약 순서가 반대라면, 모든 API 요청마다 불필요하게 ID/비밀번호 기반의 로그인을 시도하게 될 수 있습니다. JwtAuthenticationFilter를 앞에 둠으로써 "이 요청을 토큰 기반 인증인가? 그렇다면 토큰을 검증하고 처리하자. 토큰이 없다면, 다음 필터로 넘겨서 다른 방식을 찾아보자" 라는 흐름을 만들 수 있습니다.

즉 JwtAuthenticationFilter의 역할은 "만약 유효한 JWT 토큰이 있다면, 해당 사용자를 인증된 상태로 만들어준다" 입니다.
