package com.lita.orderapi.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * สร้างผู้ใช้งานเริ่มต้นให้ตอนแอปสตาร์ทครั้งแรก
 *
 * ทำที่นี่แทนการ insert ใน Flyway เพราะรหัสผ่านต้อง hash
 * ด้วย {@link PasswordEncoder} ตัวเดียวกับที่ระบบใช้ตอน login
 * จะได้ไม่มีปัญหา hash ไม่ตรงกัน
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class UserSeeder {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    @Value("${lita.seed.admin-password:admin1234}")
    private String adminPassword;

    @Value("${lita.seed.staff-password:staff1234}")
    private String staffPassword;

    @Bean
    ApplicationRunner seedDefaultUsers() {
        return args -> {
            createIfMissing("admin", adminPassword, "ผู้จัดการร้าน", Role.ADMIN);
            createIfMissing("staff", staffPassword, "พนักงานหน้าร้าน", Role.STAFF);
        };
    }

    private void createIfMissing(String username, String rawPassword, String displayName, Role role) {
        if (users.findByUsername(username).isPresent()) {
            return;
        }
        users.save(new AppUser(username, passwordEncoder.encode(rawPassword), displayName, role));
        log.info("สร้างผู้ใช้เริ่มต้น '{}' สิทธิ์ {} เรียบร้อย", username, role);
    }
}
