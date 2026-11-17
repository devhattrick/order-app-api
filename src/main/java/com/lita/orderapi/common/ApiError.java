package com.lita.orderapi.common;

import java.time.Instant;
import java.util.Map;

/**
 * รูปแบบ error response มาตรฐานของ API ตัวนี้
 *
 * @param status      HTTP status code
 * @param message     ข้อความอธิบายที่เอาไปแสดงให้ผู้ใช้ได้เลย
 * @param fieldErrors รายละเอียดราย field กรณี validation ไม่ผ่าน (ไม่มีก็เป็น null)
 * @param timestamp   เวลาที่เกิดข้อผิดพลาด
 */
public record ApiError(
        int status,
        String message,
        Map<String, String> fieldErrors,
        Instant timestamp
) {
    public static ApiError of(int status, String message) {
        return new ApiError(status, message, null, Instant.now());
    }

    public static ApiError of(int status, String message, Map<String, String> fieldErrors) {
        return new ApiError(status, message, fieldErrors, Instant.now());
    }
}
