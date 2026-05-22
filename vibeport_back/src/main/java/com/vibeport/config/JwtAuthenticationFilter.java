package com.vibeport.config;

import com.vibeport.auth.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtutil;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        // 로그인 시도나 프리플라이트는 필터를 건너뛰도록 처리
        String path = request.getRequestURI();
        if ("/vibeport/login".equals(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("authorization");

        if (header != null && header.startsWith("Bearer ")) {
            // TODO - access & refresh 한개씩 검사 필요
            String tokens = header.substring(7);
            try {
                Claims claims = this.jwtutil.parseClaims(tokens);
                // 필요하면 claims를 request attribute에 저장하거나 SecurityContext를 설정
                request.setAttribute("claims", claims);
            } catch (Exception e) {
                // 토큰 파싱/검증 실패 시 401 반환
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\":\"Invalid or expired token\"}");
                return;
            }
        }

        // 토큰이 없거나 유효한 경우(또는 위에서 처리된 로그인/OPTIONS) 다음 필터로 진행
        filterChain.doFilter(request, response);
    }
}
