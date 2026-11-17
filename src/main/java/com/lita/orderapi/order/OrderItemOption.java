package com.lita.orderapi.order;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * ตัวเลือกเสริมที่ลูกค้าเลือก — เก็บเป็น snapshot ทั้งชื่อและราคา
 *
 * ตั้งใจไม่ join กลับไปที่ตาราง option_choice ตอนแสดงผล
 * เพราะถ้าร้านขึ้นราคาท็อปปิ้งทีหลัง ใบเสร็จเก่าต้องไม่เปลี่ยนตาม
 */
@Entity
@Table(name = "order_item_option")
@Getter
@Setter
@NoArgsConstructor
public class OrderItemOption {

    @Id
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @Column(name = "group_id")
    private UUID groupId;

    @Column(name = "group_name", nullable = false)
    private String groupName;

    @Column(name = "choice_id")
    private UUID choiceId;

    @Column(name = "choice_name", nullable = false)
    private String choiceName;

    @Column(name = "price_delta", nullable = false)
    private BigDecimal priceDelta = BigDecimal.ZERO;
}
