package com.lita.orderapi.order.dto;

import com.lita.orderapi.order.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** ออเดอร์เต็มรูปแบบสำหรับฝั่งร้าน */
public record OrderDto(
        UUID id,
        String orderNo,
        String queueNo,
        String trackingToken,
        OrderType orderType,
        String tableNo,
        OrderStatus status,
        OrderSource source,
        String note,
        BigDecimal totalAmount,
        Instant paidAt,
        Instant createdAt,
        Instant updatedAt,
        List<ItemDto> items,
        List<StatusLogDto> statusHistory
) {

    public record ItemDto(
            UUID menuId,
            String menuName,
            String category,
            String emoji,
            int qty,
            BigDecimal unitPrice,
            BigDecimal lineTotal,
            String note,
            List<SelectedOptionDto> options
    ) {
    }

    public record SelectedOptionDto(
            UUID groupId,
            String groupName,
            UUID choiceId,
            String choiceName,
            BigDecimal priceDelta
    ) {
    }

    public record StatusLogDto(OrderStatus status, Instant at, String changedBy) {
    }

    public static OrderDto from(ShopOrder order) {
        return new OrderDto(
                order.getId(),
                order.getOrderNo(),
                order.getQueueNo(),
                order.getTrackingToken(),
                order.getOrderType(),
                order.getTableNo(),
                order.getStatus(),
                order.getSource(),
                order.getNote(),
                order.getTotalAmount(),
                order.getPaidAt(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getItems().stream().map(OrderDto::toItemDto).toList(),
                order.getStatusLogs().stream()
                        .map(log -> new StatusLogDto(log.getStatus(), log.getAt(), log.getChangedBy()))
                        .toList()
        );
    }

    private static ItemDto toItemDto(OrderItem item) {
        return new ItemDto(
                item.getMenuId(),
                item.getMenuName(),
                item.getCategory(),
                item.getEmoji(),
                item.getQty(),
                item.getUnitPrice(),
                item.getLineTotal(),
                item.getNote(),
                item.getOptions().stream()
                        .map(option -> new SelectedOptionDto(
                                option.getGroupId(),
                                option.getGroupName(),
                                option.getChoiceId(),
                                option.getChoiceName(),
                                option.getPriceDelta()))
                        .toList()
        );
    }
}
