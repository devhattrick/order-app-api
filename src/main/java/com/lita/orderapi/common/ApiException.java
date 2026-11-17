package com.lita.orderapi.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Exception สำหรับข้อผิดพลาดที่เรา "ตั้งใจ" ให้เกิด เช่น หาเมนูไม่เจอ
 * หรือเปลี่ยนสถานะออเดอร์ข้ามขั้น — จะถูกแปลงเป็น JSON response
 * โดย {@link GlobalExceptionHandler}
 */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }

    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }
}
