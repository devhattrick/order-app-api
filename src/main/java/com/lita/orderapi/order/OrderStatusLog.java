package com.lita.orderapi.order;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** ประวัติการเปลี่ยนสถานะ ใช้ทำ timeline ให้ลูกค้าดูและตรวจย้อนหลังได้ */
@Entity
@Table(name = "order_status_log")
@Getter
@Setter
@NoArgsConstructor
public class OrderStatusLog {

    @Id
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private ShopOrder order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    /** username ของคนที่กด — null ถ้าระบบเปลี่ยนให้เอง */
    @Column(name = "changed_by")
    private String changedBy;

    @Column(nullable = false)
    private Instant at = Instant.now();

    public OrderStatusLog(ShopOrder order, OrderStatus status, String changedBy) {
        this.id = UUID.randomUUID();
        this.order = order;
        this.status = status;
        this.changedBy = changedBy;
        this.at = Instant.now();
    }
}
