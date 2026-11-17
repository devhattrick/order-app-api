package com.lita.orderapi.shoptable;

import com.lita.orderapi.common.ApiException;
import com.lita.orderapi.order.*;
import com.lita.orderapi.order.dto.OrderDto;
import com.lita.orderapi.shoptable.dto.TableStateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShopTableService {

    /** เรียงตามความเร่งด่วน — ตัวแรกที่เจอคือสถานะที่จะโชว์บนการ์ดโต๊ะ */
    private static final List<OrderStatus> STAGE_PRIORITY =
            List.of(OrderStatus.PENDING, OrderStatus.PREPARING, OrderStatus.READY, OrderStatus.SERVED);

    private final ShopTableProperties properties;
    private final OrderRepository orders;

    @Transactional(readOnly = true)
    public List<TableStateDto> getAllTableStates() {
        return properties.getTables().stream()
                .map(table -> buildState(table.getTableNo(), table.getSeats()))
                .toList();
    }

    private TableStateDto buildState(String tableNo, int seats) {
        List<ShopOrder> active =
                orders.findByTableNoAndStatusInOrderByCreatedAtAsc(tableNo, OrderStatus.ACTIVE);

        if (active.isEmpty()) {
            return new TableStateDto(tableNo, seats, "FREE", 0, BigDecimal.ZERO, null, List.of());
        }

        String stage = STAGE_PRIORITY.stream()
                .filter(status -> active.stream().anyMatch(order -> order.getStatus() == status))
                .findFirst()
                .map(Enum::name)
                .orElse("SERVED");

        BigDecimal total = active.stream()
                .map(ShopOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Instant since = active.stream()
                .map(ShopOrder::getCreatedAt)
                .min(Comparator.naturalOrder())
                .orElse(null);

        return new TableStateDto(
                tableNo,
                seats,
                stage,
                active.size(),
                total,
                since,
                active.stream().map(OrderDto::from).toList()
        );
    }

    /**
     * เช็คบิลและเก็บโต๊ะ — ปิดทุกออเดอร์ที่ยังค้างอยู่ของโต๊ะนั้นพร้อมกัน
     *
     * นี่คือจุดเดียวที่ออเดอร์ DINE_IN จะกลายเป็น COMPLETED ได้
     * เพราะเงินเข้าร้านตอนนี้ ไม่ใช่ตอนเสิร์ฟ
     */
    @Transactional
    public List<ShopOrder> closeTable(String tableNo, String username) {
        List<ShopOrder> active =
                orders.findByTableNoAndStatusInOrderByCreatedAtAsc(tableNo, OrderStatus.ACTIVE);

        if (active.isEmpty()) {
            throw ApiException.conflict("โต๊ะ %s ไม่มีออเดอร์ค้างให้เช็คบิล".formatted(tableNo));
        }

        Instant paidAt = Instant.now();
        active.forEach(order -> {
            order.changeStatus(OrderStatus.COMPLETED, username);
            if (!order.isPaid()) {
                order.setPaidAt(paidAt);
            }
        });

        return orders.saveAll(active);
    }
}
