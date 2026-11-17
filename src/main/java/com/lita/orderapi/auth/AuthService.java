package com.lita.orderapi.auth;

import com.lita.orderapi.auth.dto.LoginRequest;
import com.lita.orderapi.auth.dto.LoginResponse;
import com.lita.orderapi.auth.dto.UserProfile;
import com.lita.orderapi.common.ApiException;
import com.lita.orderapi.user.AppUser;
import com.lita.orderapi.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {
        AppUser user = users.findByUsername(request.username())
                .orElseThrow(this::invalidCredentials);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        if (!user.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "บัญชีนี้ถูกระงับการใช้งาน");
        }

        return new LoginResponse(
                jwtService.issueToken(user),
                jwtService.getExpirationSeconds(),
                UserProfile.from(user)
        );
    }

    public UserProfile getProfile(String username) {
        return users.findByUsername(username)
                .map(UserProfile::from)
                .orElseThrow(() -> ApiException.notFound("ไม่พบผู้ใช้งาน " + username));
    }

    /**
     * ตอบข้อความเดียวกันทั้งกรณีไม่มี username และรหัสผ่านผิด
     * เพื่อไม่ให้คนเดารู้ว่า username ไหนมีอยู่จริงในระบบ
     */
    private ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "ชื่อผู้ใช้หรือรหัสผ่านไม่ถูกต้อง");
    }
}
