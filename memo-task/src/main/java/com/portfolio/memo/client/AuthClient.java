package com.portfolio.memo.client;

import com.portfolio.memo.client.dto.UserSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AuthClient {

    private final WebClient webClient;

    public boolean existsUser(Long userId) {
        return webClient.get()
                .uri("/api/auth/users/{userId}/exists", userId)
                .retrieve()
                .bodyToMono(Boolean.class)
                .defaultIfEmpty(false)
                .block();
    }

    public String getUsername(Long userId) {
        return webClient.get()
                .uri("/api/auth/users/{userId}/name", userId)
                .retrieve()
                .bodyToMono(String.class)
                // onErrorReturn(null)은 fallback 값으로 null을 허용하지 않아 항상 NPE가 발생함.
                // onErrorResume으로 빈 Mono를 반환하면 block()이 정상적으로 null을 돌려준다.
                .onErrorResume(ex -> Mono.empty())
                .block();
    }

    // 여러 유저를 한 번에 조회 (팀원 수만큼 blocking 호출이 발생하는 것을 방지)
    public List<UserSummaryDto> getUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        return webClient.post()
                .uri("/api/auth/users/batch")
                .bodyValue(userIds)
                .retrieve()
                .bodyToFlux(UserSummaryDto.class)
                .collectList()
                .onErrorReturn(List.of())
                .block();
    }

}
