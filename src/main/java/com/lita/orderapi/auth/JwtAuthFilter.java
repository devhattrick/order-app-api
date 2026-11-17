package com.lita.orderapi.auth;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * อ่าน JWT จาก header {@code Authorization: Bearer <token>}
 * แล้วบอก Spring Security ว่า request นี้เป็นใครและมีสิทธิ์อะไร
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);

        // ไม่มี token ก็ปล่อยผ่านไป ให้ SecurityConfig เป็นคนตัดสินว่า endpoint นี้ต้อง login ไหม
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Claims claims = jwtService.parseToken(token);

            if (claims != null) {
                String username = claims.getSubject();
                String role = claims.get("role", String.class);

                var authentication = new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * อ่าน token จาก header เท่านั้น
     *
     * <p>เคยรองรับ {@code ?token=} ใน query string ด้วย เพราะ EventSource
     * ที่ใช้ทำ SSE ใส่ custom header ไม่ได้ แต่ตอนนี้เปลี่ยนไปใช้ polling แล้ว
     * จึงถอดออกเพื่อความปลอดภัย — token ที่อยู่ใน URL มีโอกาสติดไปกับ
     * access log, browser history และ Referer header ส่วนที่อยู่ใน header ไม่ติด
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(PREFIX)) {
            return header.substring(PREFIX.length());
        }
        return null;
    }
}
