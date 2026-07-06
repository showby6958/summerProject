package com.portfolio.memo.client;

import com.portfolio.memo.client.dto.UserSummaryDto;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;

// memo-task -> memo-auth 팀원 조회 방식 개선(N+1 -> 배치 API 1회)
// 실제 네트워크 왕복을 흉내 낸 가짜 서버(MockWebServer)로 재현해서 검증
// "개별 호출 방식"은 AuthClient.existsUser()/getUsername()을 팀원 수만큼 반복 호출한다.
// "배치 API 방식"은 현재 프로덕션 코드(TaskService.resolveMemberNames)가 실제로 사용하는 AuthClient.getUsers() 한 번 호출
class AuthClientBatchPerformanceTest {

    // 가짜 서버 응답에 인위적인 지연을 줘서, 로컬 루프백이라도 실제 네트워크 왕복처럼
    // 요청 횟수에 비례해 시간이 늘어나는 걸 재현한다.
    private static final long NETWORK_DELAY_MS = 50;

    private MockWebServer mockWebServer;

    @AfterEach
    void tearDown() throws IOException {
        if (mockWebServer != null) {
            mockWebServer.shutdown();
        }
    }

    @ParameterizedTest(name = "팀원 수 = {0}명")
    @ValueSource(ints = {1, 5, 10, 20})
    void 개별_호출_방식은_팀원_수에_비례해_요청과_시간이_늘어난다(int memberCount) throws IOException {
        List<Long> memberIds = memberIds(memberCount);
        AuthClient authClient = startServerAndCreateClient(memberIds);

        long start = System.nanoTime();
        for (Long memberId : memberIds) {
            // 리팩터링 전 TaskService.resolveMemberNames()가 하던 것과 동일한 패턴
            assertThat(authClient.existsUser(memberId)).isTrue();
            assertThat(authClient.getUsername(memberId)).isNotNull();
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        int expectedRequestCount = memberCount * 2; // exists 1회 + name 1회
        assertThat(mockWebServer.getRequestCount()).isEqualTo(expectedRequestCount);

        System.out.printf(
                "[개별 호출 방식] 팀원 수=%2d, HTTP 요청 수=%3d, 소요시간=%4dms%n",
                memberCount, expectedRequestCount, elapsedMs
        );
    }

    @ParameterizedTest(name = "팀원 수 = {0}명")
    @ValueSource(ints = {1, 5, 10, 20})
    void 배치_API_방식은_팀원_수와_무관하게_요청이_1번이다(int memberCount) throws IOException {
        List<Long> memberIds = memberIds(memberCount);
        AuthClient authClient = startServerAndCreateClient(memberIds);

        long start = System.nanoTime();
        List<UserSummaryDto> result = authClient.getUsers(memberIds);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(result).hasSize(memberCount);
        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);

        System.out.printf(
                "[배치 API 방식]   팀원 수=%2d, HTTP 요청 수=%3d, 소요시간=%4dms%n",
                memberCount, 1, elapsedMs
        );
    }

    private List<Long> memberIds(int count) {
        return LongStream.rangeClosed(1, count).boxed().toList();
    }

    private AuthClient startServerAndCreateClient(List<Long> memberIds) throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.setDispatcher(fakeAuthServiceDispatcher(memberIds));
        mockWebServer.start();

        WebClient webClient = WebClient.builder()
                .baseUrl("http://localhost:" + mockWebServer.getPort())
                .build();

        return new AuthClient(webClient);
    }

    // memo-auth가 실제로 응답하는 형식(exists: Boolean, name: String, batch: UserSummaryDto[])을 그대로 흉내 낸다.
    private Dispatcher fakeAuthServiceDispatcher(List<Long> memberIds) {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath();
                if (path == null) {
                    return new MockResponse().setResponseCode(404);
                }

                if (path.endsWith("/exists")) {
                    return jsonResponse("true");
                }
                if (path.endsWith("/name")) {
                    return jsonResponse("\"테스트유저\"");
                }
                if (path.endsWith("/batch")) {
                    String body = memberIds.stream()
                            .map(id -> "{\"id\":" + id + ",\"name\":\"테스트유저-" + id + "\"}")
                            .collect(Collectors.joining(",", "[", "]"));
                    return jsonResponse(body);
                }
                return new MockResponse().setResponseCode(404);
            }
        };
    }

    private MockResponse jsonResponse(String body) {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(body)
                .setBodyDelay(NETWORK_DELAY_MS, TimeUnit.MILLISECONDS);
    }
}