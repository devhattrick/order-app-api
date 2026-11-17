package com.lita.orderapi.order.dto;

import com.lita.orderapi.order.OrderStatus;
import com.lita.orderapi.order.OrderType;
import com.lita.orderapi.order.ShopOrder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * ข้อมูลออเดอร์เวอร์ชันสาธารณะ — ใช้ตอนลูกค้าสแกน QR
 *
 * ตัดสิ่งที่ลูกค้าไม่ควรเห็นออก เช่น id ภายใน ช่องทางที่สั่ง
 * และชื่อพนักงานที่กดเปลี่ยนสถานะ
 */
public record PublicOrderDto(
        String orderNo,
        String queueNo,
        OrderType orderType,
        String tableNo,
        OrderStatus status,
        BigDecimal totalAmount,
        boolean paid,
        Instant createdAt,
        List<ItemLine> items,
        List<Step> timeline
) {

    public record ItemLine(String menuName, int qty, BigDecimal lineTotal, List<String> options) {
    }

    public record Step(OrderStatus status, Instant at) {
    }

    public static PublicOrderDto from(ShopOrder order) {
        return new PublicOrderDto(
                order.getOrderNo(),
                order.getQueueNo(),
                order.getOrderType(),
                order.getTableNo(),
                order.getStatus(),
                order.getTotalAmount(),
                order.isPaid(),
                order.getCreatedAt(),
                order.getItems().stream()
                        .map(item -> new ItemLine(
                                item.getMenuName(),
                                item.getQty(),
                                item.getLineTotal(),
                                item.getOptions().stream().map(o -> o.getChoiceName()).toList()))
                        .toList(),
                order.getStatusLogs().stream()
                        .map(log -> new Step(log.getStatus(), log.getAt()))
                        .toList()
        );
    }
}
