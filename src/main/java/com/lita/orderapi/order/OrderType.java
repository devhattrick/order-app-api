package com.lita.orderapi.order;

/**
 * ประเภทออเดอร์ — เป็นตัวกำหนดทั้ง flow สถานะและจังหวะชำระเงิน
 *
 * <ul>
 *   <li>{@code DINE_IN} ต้องระบุโต๊ะ · ชำระเงินตอนเช็คบิล (postpaid)</li>
 *   <li>{@code TAKEAWAY} ไม่มีโต๊ะ · ชำระเงินตอนสั่ง (prepaid)</li>
 * </ul>
 */
public enum OrderType {
    DINE_IN,
    TAKEAWAY
}
