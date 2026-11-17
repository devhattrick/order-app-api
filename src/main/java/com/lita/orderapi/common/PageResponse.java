package com.lita.orderapi.common;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * รูปแบบผลลัพธ์แบบแบ่งหน้าของ API ตัวนี้
 *
 * <p>ไม่คืน {@link Page} ของ Spring Data ออกไปตรง ๆ เพราะ Spring เตือนไว้เองว่า
 * โครงสร้าง JSON ของ PageImpl ไม่การันตีว่าจะเหมือนเดิมข้ามเวอร์ชัน
 * (ดู warning "Serializing PageImpl instances as-is is not supported")
 * ถ้า upgrade Spring Boot แล้วรูปแบบเปลี่ยน frontend จะพังโดยไม่มีใครรู้ล่วงหน้า
 *
 * <p>ประกาศเองแบบนี้ contract เป็นของเรา อยากเปลี่ยนเมื่อไหร่ก็รู้ตัวว่าต้องแก้ frontend ด้วย
 *
 * @param content      ข้อมูลของหน้านี้
 * @param page         เลขหน้าปัจจุบัน เริ่มที่ 0
 * @param size         จำนวนรายการต่อหน้า
 * @param totalElements จำนวนรายการทั้งหมดที่ตรงเงื่อนไข
 * @param totalPages   จำนวนหน้าทั้งหมด
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> PageResponse<T> from(Page<T> source) {
        return new PageResponse<>(
                source.getContent(),
                source.getNumber(),
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages()
        );
    }
}
