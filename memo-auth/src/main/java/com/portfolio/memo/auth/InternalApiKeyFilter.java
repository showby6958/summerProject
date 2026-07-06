package com.portfolio.memo.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// 서비스 간 내부 호출 전용 API(/api/auth/users/**)를 유저 JWT 없이도 쓸 수 있게 permitAll 했는데,
// 그러면 외부에서도 그대로 호출해 유저 존재 여부/이름을 열람할 수 있어서 내부 API 키로 별도 보호한다.
@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME = "X-Internal-Api-Key";
    private static final String PROTECTED_PREFIX = "/api/auth/users/";

    @Value("${internal-api.key}")
    private String internalApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        if (request.getRequestURI().startsWith(PROTECTED_PREFIX)) {
            String key = request.getHeader(HEADER_NAME);
            if (key == null || !key.equals(internalApiKey)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid internal API key");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
