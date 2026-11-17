package com.lita.orderapi.auth.dto;

import com.lita.orderapi.user.AppUser;
import com.lita.orderapi.user.Role;

/** ข้อมูลผู้ใช้ที่ส่งให้ frontend เอาไปซ่อน/แสดงเมนูตามสิทธิ์ */
public record UserProfile(
        String username,
        String displayName,
        Role role
) {
    public static UserProfile from(AppUser user) {
        return new UserProfile(user.getUsername(), user.getDisplayName(), user.getRole());
    }
}
