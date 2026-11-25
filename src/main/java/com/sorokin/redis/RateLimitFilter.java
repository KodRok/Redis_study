package com.sorokin.redis;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final FixedWindowRateLimiter rateLimiter;

    private static final int MAX_REQUESTS = 10;
    private static final int WINDOW_SECONDS = 60;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Ограничиваем только API-эндпоинты
        if (!request.getRequestURI().startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Берем X-API-KEY, если передан, иначе используем IP-адрес клиента
        String clientId = request.getHeader("X-API-KEY");
        if (clientId == null || clientId.isBlank()) {
            clientId = request.getRemoteAddr();
        }

        boolean allowed = rateLimiter.isAllowed(clientId, MAX_REQUESTS, WINDOW_SECONDS);

        if (!allowed) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("429 Too Many Requests: превышен лимит 10 запросов в минуту");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
