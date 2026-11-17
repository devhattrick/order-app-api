package com.lita.orderapi.order;

import com.lita.orderapi.common.ApiException;
import com.lita.orderapi.common.PageResponse;
import com.lita.orderapi.order.dto.CreateOrderRequest;
import com.lita.orderapi.order.dto.OrderDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** ออเดอร์ฝั่งร้าน — ต้อง login ทุก endpoint (STAFF และ ADMIN ใช้ได้เท่ากัน) */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDto create(@Valid @RequestBody CreateOrderRequest request, Principal principal) {
        return OrderDto.from(orderService.create(request, principal.getName()));
    }

    /** ออเดอร์ที่ยังไม่จบทั้งหมด — กระดานครัวเรียกตอนเปิดหน้า */
    @GetMapping("/active")
    public List<OrderDto> active() {
        return orderService.findActive().stream().map(OrderDto::from).toList();
    }

    @GetMapping("/{id}")
    public OrderDto getOne(@PathVariable UUID id) {
        return OrderDto.from(orderService.findById(id));
    }

    /**
     * เปลี่ยนสถานะทีละขั้นตาม flow — ส่ง CANCELLED มาเพื่อยกเลิกได้ทุกตอน
     * ที่ออเดอร์ยังไม่จบ
     */
    @PatchMapping("/{id}/status")
    public OrderDto changeStatus(@PathVariable UUID id,
                                 @RequestBody Map<String, String> body,
                                 Principal principal) {
        return OrderDto.from(orderService.changeStatus(id, parseStatus(body.get("status")), principal.getName()));
    }

    /** ประวัติออเดอร์ พร้อม filter และแบ่งหน้า */
    @GetMapping
    public PageResponse<OrderDto> search(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) OrderType orderType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {

        Instant fromInstant = from == null
                ? null
                : from.atStartOfDay(DocumentNumberService.SHOP_ZONE).toInstant();
        Instant toInstant = to == null
                ? null
                : to.plusDays(1).atStartOfDay(DocumentNumberService.SHOP_ZONE).toInstant();

        Page<OrderDto> result = orderService
                .search(fromInstant, toInstant, status, orderType, keyword,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(OrderDto::from);

        return PageResponse.from(result);
    }

    private OrderStatus parseStatus(String raw) {
        try {
            return OrderStatus.valueOf(raw);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw ApiException.badRequest("สถานะ \"%s\" ไม่ถูกต้อง".formatted(raw));
        }
    }

}
