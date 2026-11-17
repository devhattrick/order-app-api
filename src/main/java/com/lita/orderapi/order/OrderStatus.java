package com.lita.orderapi.order;

import java.util.List;
import java.util.Set;

/**
 * สถานะออเดอร์ และกติกาว่าเปลี่ยนไปสถานะไหนต่อได้บ้าง
 *
 * <pre>
 * DINE_IN  : PENDING → PREPARING → READY → SERVED → COMPLETED
 *                                                   (เฉพาะตอนเช็คบิลที่ผังโต๊ะ)
 * TAKEAWAY : PENDING → PREPARING → READY → COMPLETED
 * </pre>
 *
 * ทุกสถานะที่ยังไม่จบ ยกเลิกเป็น CANCELLED ได้
 */
public enum OrderStatus {

    /** ส่งเข้าครัวแล้ว รอครัวรับ */
    PENDING,
    /** ครัวกำลังทำ */
    PREPARING,
    /** ทำเสร็จ พร้อมเสิร์ฟหรือพร้อมให้ลูกค้ารับ */
    READY,
    /** เสิร์ฟถึงโต๊ะแล้วแต่ยังไม่ชำระเงิน — ใช้เฉพาะ DINE_IN */
    SERVED,
    /** ปิดบิล ชำระเงินเรียบร้อย */
    COMPLETED,
    /** ยกเลิก ไม่นับเป็นยอดขาย */
    CANCELLED;

    private static final Set<OrderStatus> TERMINAL = Set.of(COMPLETED, CANCELLED);

    /** สถานะที่ถือว่าออเดอร์ยังเปิดอยู่ — ใช้ตัดสินว่าโต๊ะว่างหรือยัง */
    public static final List<OrderStatus> ACTIVE = List.of(PENDING, PREPARING, READY, SERVED);

    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }

    /**
     * สถานะถัดไปตาม flow ปกติ — คืน null ถ้าเดินต่อเองไม่ได้
     * (เช่น SERVED ต้องรอเช็คบิลที่ผังโต๊ะเท่านั้น)
     */
    public OrderStatus next(OrderType orderType) {
        return switch (this) {
            case PENDING -> PREPARING;
            case PREPARING -> READY;
            case READY -> orderType == OrderType.DINE_IN ? SERVED : COMPLETED;
            default -> null;
        };
    }
}
