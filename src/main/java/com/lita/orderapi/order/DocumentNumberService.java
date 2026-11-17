package com.lita.orderapi.order;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * ออกเลขที่เอกสารและเลขคิวแบบรันรายวัน
 *
 * <p>จุดสำคัญคือ <b>ต้องออกเลขที่ฝั่ง server เท่านั้น</b> — ตอนเป็น demo
 * ฝั่ง frontend คำนวณเลขเองจาก localStorage ซึ่งถ้าพนักงานสองเครื่อง
 * กดพร้อมกันจะได้เลขคิวซ้ำกัน
 *
 * <p>ใช้ {@code INSERT ... ON CONFLICT DO UPDATE ... RETURNING} ของ Postgres
 * ซึ่งทำงานเป็น atomic operation เดียว ไม่ต้องล็อกตารางเอง และปลอดภัย
 * แม้จะมีหลาย instance ของ API รันพร้อมกัน
 */
@Service
@RequiredArgsConstructor
public class DocumentNumberService {

    /** ใช้เวลาไทยตัดวัน เพราะร้านปิดก่อนเที่ยงคืนอยู่แล้ว */
    public static final ZoneId SHOP_ZONE = ZoneId.of("Asia/Bangkok");

    private static final DateTimeFormatter ORDER_NO_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final String SCOPE_ORDER_NO = "ORDER_NO";
    private static final String SCOPE_QUEUE_DINE_IN = "QUEUE_DINE_IN";
    private static final String SCOPE_QUEUE_TAKEAWAY = "QUEUE_TAKEAWAY";

    private final EntityManager entityManager;

    /** เช่น 20261117-007 */
    public String nextOrderNo(LocalDate businessDate) {
        int running = nextValue(businessDate, SCOPE_ORDER_NO);
        return "%s-%03d".formatted(businessDate.format(ORDER_NO_DATE), running);
    }

    /** เช่น D-012 (ทานที่ร้าน) หรือ T-004 (ซื้อกลับ) */
    public String nextQueueNo(LocalDate businessDate, OrderType orderType) {
        boolean dineIn = orderType == OrderType.DINE_IN;
        int running = nextValue(businessDate, dineIn ? SCOPE_QUEUE_DINE_IN : SCOPE_QUEUE_TAKEAWAY);
        return "%s-%03d".formatted(dineIn ? "D" : "T", running);
    }

    public static LocalDate today() {
        return LocalDate.now(SHOP_ZONE);
    }

    /**
     * บวกตัวนับของ (วันที่, scope) ขึ้น 1 แล้วคืนค่าใหม่
     *
     * <p>รันอยู่ใน transaction ของคนเรียก (OrderService.create) — Postgres
     * จะล็อกแถวตัวนับไว้จนกว่าจะ commit ทำให้ออเดอร์ที่เข้ามาพร้อมกัน
     * ต่อคิวกันเองโดยอัตโนมัติ ไม่มีทางได้เลขซ้ำ
     */
    int nextValue(LocalDate businessDate, String scope) {
        Object value = entityManager.createNativeQuery("""
                        insert into daily_counter (business_date, scope, value)
                        values (:businessDate, :scope, 1)
                        on conflict (business_date, scope)
                        do update set value = daily_counter.value + 1
                        returning value
                        """)
                .setParameter("businessDate", businessDate)
                .setParameter("scope", scope)
                .getSingleResult();

        return ((Number) value).intValue();
    }
}
