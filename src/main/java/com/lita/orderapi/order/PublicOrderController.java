package com.lita.orderapi.order;

import com.lita.orderapi.order.dto.PublicOrderDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ช่องทางเดียวที่เปิดให้เรียกโดยไม่ต้อง login
 *
 * ลูกค้าสแกน QR บนใบเสร็จแล้วเปิดหน้านี้เพื่อดูว่าอาหารถึงขั้นไหนแล้ว
 * ใช้ trackingToken ที่สุ่มมาแทน id จริง และส่งกลับเฉพาะข้อมูลที่ลูกค้าควรเห็น
 */
@RestController
@RequestMapping("/api/public/orders")
@RequiredArgsConstructor
public class PublicOrderController {

    private final OrderService orderService;

    @GetMapping("/{trackingToken}")
    public PublicOrderDto track(@PathVariable String trackingToken) {
        return PublicOrderDto.from(orderService.findByTrackingToken(trackingToken));
    }
}
