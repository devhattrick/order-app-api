package com.lita.orderapi.shoptable.dto;

import com.lita.orderapi.order.dto.OrderDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * สถานะของโต๊ะหนึ่งตัว
 *
 * @param stage สถานะรวมของโต๊ะ — FREE ถ้าไม่มีออเดอร์ค้าง
 *              ถ้ามีหลายออเดอร์จะเอาสถานะที่ "เร่งด่วนที่สุด" มาแสดง
 *              (รอรับออเดอร์ มาก่อน กำลังทำ มาก่อน พร้อมเสิร์ฟ มาก่อน รอเช็คบิล)
 */
public record TableStateDto(
        String tableNo,
        int seats,
        String stage,
        int orderCount,
        BigDecimal total,
        Instant occupiedSince,
        List<OrderDto> orders
) {
}
