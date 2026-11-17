package com.lita.orderapi.auth;

import com.lita.orderapi.user.AppUser;
import com.lita.orderapi.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** บอก Spring Security ว่าจะไปหา user จากที่ไหน */
@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository users;

    @Override
    public UserDetails loadUserByUsername(String username) {
        AppUser user = users.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("ไม่พบผู้ใช้งาน " + username));

        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                // Spring Security คาดหวัง prefix "ROLE_" เพื่อให้ hasRole("ADMIN") ทำงาน
                .authorities("ROLE_" + user.getRole().name())
                .disabled(!user.isActive())
                .build();
    }
}
