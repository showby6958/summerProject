package com.portfolio.memo.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class AuthClient {

    private final WebClient webClient;

    public boolean existsUser(Long userId) {
        return webClient.get()
                .uri("http://localhost:8081/api/auth/users/{userId}/exists", userId)
                .retrieve()
                .bodyToMono(Boolean.class)
                .defaultIfEmpty(false)
                .block();
    }

    public String getUsername(Long userId) {
        return webClient.get()
                .uri("http://localhost:8081/api/auth/users/{userId}/name", userId)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

}
