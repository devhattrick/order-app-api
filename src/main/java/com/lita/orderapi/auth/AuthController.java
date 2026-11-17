package com.lita.orderapi.auth;

import com.lita.orderapi.auth.dto.LoginRequest;
import com.lita.orderapi.auth.dto.LoginResponse;
import com.lita.orderapi.auth.dto.UserProfile;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** เข้าสู่ระบบ — endpoint เดียวที่เปิดให้เรียกโดยไม่ต้องมี token */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** ดูว่าตอนนี้ login ด้วยบัญชีอะไร สิทธิ์อะไร — frontend ใช้ตอนเปิดแอป */
    @GetMapping("/me")
    public UserProfile me(Principal principal) {
        return authService.getProfile(principal.getName());
    }
}
