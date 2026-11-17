package com.lita.orderapi.order;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * ออเดอร์หนึ่งใบ
 *
 * ตั้งชื่อว่า ShopOrder เพราะ "order" เป็น reserved word ของ SQL
 */
@Entity
@Table(name = "shop_order")
@Getter
@Setter
@NoArgsConstructor
public class ShopOrder {

    @Id
    private UUID id = UUID.randomUUID();

    /** เลขที่เอกสาร รูปแบบ YYYYMMDD-NNN รันรวมทั้งร้าน รีเซ็ตทุกวัน */
    @Column(name = "order_no", nullable = false, unique = true)
    private String orderNo;

    /** เลขคิวที่ลูกค้าเห็น D-001 (ทานที่ร้าน) หรือ T-001 (ซื้อกลับ) */
    @Column(name = "queue_no", nullable = false)
    private String queueNo;

    /**
     * โทเคนสุ่มสำหรับ QR ติดตามสถานะ — ใช้แทน id ตรง ๆ
     * เพราะใบเสร็จเป็นกระดาษที่ใครเก็บไปก็เปิดดูได้ ไม่ควรเดา id ของระบบได้
     */
    @Column(name = "tracking_token", nullable = false, unique = true)
    private String trackingToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false)
    private OrderType orderType;

    /** มีค่าเฉพาะ DINE_IN — ฐานข้อมูลมี check constraint บังคับไว้อีกชั้น */
    @Column(name = "table_no")
    private String tableNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderSource source = OrderSource.POS;

    private String note;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /** เวลาที่ชำระเงิน — ซื้อกลับ = ตอนสั่ง, ทานที่ร้าน = ตอนเช็คบิล */
    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("sortOrder asc")
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("at asc")
    private List<OrderStatusLog> statusLogs = new ArrayList<>();

    public void addItem(OrderItem item) {
        item.setOrder(this);
        item.setSortOrder(this.items.size());
        this.items.add(item);
    }

    /** เปลี่ยนสถานะพร้อมบันทึกประวัติในที่เดียว จะได้ไม่มีใครลืมเขียน log */
    public void changeStatus(OrderStatus newStatus, String changedBy) {
        this.status = newStatus;
        this.updatedAt = Instant.now();
        this.statusLogs.add(new OrderStatusLog(this, newStatus, changedBy));
    }

    public boolean isPaid() {
        return paidAt != null;
    }
}
