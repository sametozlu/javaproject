package com.ecommerce.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();
    private final Map<String, Long> windowStart = new ConcurrentHashMap<>();

    @Value("${app.rate-limit.auth-max:10}")
    private int authMax;

    @Value("${app.rate-limit.window-ms:60000}")
    private long windowMs;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.startsWith("/api/auth/")) {
            String key = request.getRemoteAddr() + ":" + path;
            if (isLimited(key, authMax)) {
                response.setStatus(429);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"message\":\"Too many requests. Try again later.\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private boolean isLimited(String key, int max) {
        long now = System.currentTimeMillis();
        windowStart.putIfAbsent(key, now);
        counters.putIfAbsent(key, new AtomicInteger(0));

        if (now - windowStart.get(key) > windowMs) {
            windowStart.put(key, now);
            counters.put(key, new AtomicInteger(0));
        }
        return counters.get(key).incrementAndGet() > max;
    }
}
