package com.lita.orderapi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lita.orderapi.auth.JwtAuthFilter;
import com.lita.orderapi.common.ApiError;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * กติกาสิทธิ์ของทั้งระบบรวมอยู่ที่ไฟล์นี้ไฟล์เดียว
 *
 * <pre>
 * เปิดสาธารณะ  POST /api/auth/login        เข้าสู่ระบบ
 *              GET  /api/public/orders/**  ลูกค้าสแกน QR ดูสถานะ (ไม่ต้อง login)
 *
 * ADMIN เท่านั้น  /api/dashboard/**                 ดูยอดขายและรายงาน
 *                POST/PUT/DELETE /api/menus/**      เพิ่ม/แก้ไข/ลบเมนู
 *                POST/PUT/DELETE /api/option-groups/**  จัดการตัวเลือกเสริม
 *                POST/PUT/DELETE /api/categories/**     จัดการหมวดหมู่เมนู
 *
 * STAFF + ADMIN  ที่เหลือทั้งหมด เช่น รับออเดอร์ เปลี่ยนสถานะ เช็คบิล
 *                รวมถึง PATCH /api/menus/{id}/availability เพื่อกดปิดขายตอนของหมด
 * </pre>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final ObjectMapper objectMapper;

    @Value("${lita.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // ไม่ใช้ session และส่ง token มาทาง header อยู่แล้ว จึงไม่มีช่องให้ CSRF
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        /*
                         * ปล่อย ERROR dispatch ผ่านไป เพื่อให้ Spring Boot render หน้า error
                         * ของตัวเองได้ ไม่งั้น error เล็ก ๆ จะกลายเป็น 403 ที่ไล่หาสาเหตุยาก
                         *
                         * ไม่ใช่ช่องโหว่ เพราะ ERROR dispatch เกิดได้เฉพาะกับ request
                         * ที่ผ่าน filter chain รอบแรกมาแล้วเท่านั้น
                         */
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()

                        .requestMatchers("/api/dashboard/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.POST, "/api/menus/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/menus/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/menus/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.POST, "/api/option-groups/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/option-groups/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/option-groups/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.POST, "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )

                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, ex) ->
                                writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "กรุณาเข้าสู่ระบบก่อนใช้งาน"))
                        .accessDeniedHandler((request, response, ex) ->
                                writeError(response, HttpServletResponse.SC_FORBIDDEN, "บัญชีนี้ไม่มีสิทธิ์ใช้งานส่วนนี้"))
                )

                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private void writeError(HttpServletResponse response, int status, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiError.of(status, message));
    }
}
