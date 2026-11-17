package com.lita.orderapi.order;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * ตัวกรองสำหรับหน้าประวัติออเดอร์
 *
 * <p>ใช้ Specification แทนการเขียน JPQL ที่มี {@code :param is null or ...}
 * เพราะแบบนั้นต้องส่ง parameter ครบทุกตัวแม้ไม่ได้กรอง และ Hibernate
 * เดาชนิดข้อมูลของ null ไม่ได้ในบางเคส วิธีนี้จะต่อเฉพาะเงื่อนไขที่ถูกใช้จริง
 */
public final class OrderSpecifications {

    private OrderSpecifications() {
    }

    public static Specification<ShopOrder> filter(Instant from,
                                                  Instant to,
                                                  OrderStatus status,
                                                  OrderType orderType,
                                                  String keyword) {

        return (root, query, builder) -> {
            List<Predicate> conditions = new ArrayList<>();

            if (from != null) {
                conditions.add(builder.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                conditions.add(builder.lessThan(root.get("createdAt"), to));
            }
            if (status != null) {
                conditions.add(builder.equal(root.get("status"), status));
            }
            if (orderType != null) {
                conditions.add(builder.equal(root.get("orderType"), orderType));
            }
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                conditions.add(builder.or(
                        builder.like(builder.lower(root.get("orderNo")), pattern),
                        builder.like(builder.lower(root.get("queueNo")), pattern),
                        builder.like(builder.lower(builder.coalesce(root.get("tableNo"), "")), pattern)
                ));
            }

            return builder.and(conditions.toArray(new Predicate[0]));
        };
    }
}
