package com.lita.orderapi.order;

import com.lita.orderapi.menu.Category;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** หนึ่งรายการในออเดอร์ — เก็บชื่อและราคา ณ เวลาที่สั่ง */
@Entity
@Table(name = "order_item")
@Getter
@Setter
@NoArgsConstructor
public class OrderItem {

    @Id
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private ShopOrder order;

    /** อ้างกลับไปที่เมนูต้นทาง เผื่อทำรายงานเมนูขายดี (เมนูอาจถูกลบไปแล้ว) */
    @Column(name = "menu_id")
    private UUID menuId;

    @Column(name = "menu_name", nullable = false)
    private String menuName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    private String emoji;

    @Column(nullable = false)
    private int qty;

    /** ราคาต่อชิ้นรวมตัวเลือกเสริมแล้ว */
    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "line_total", nullable = false)
    private BigDecimal lineTotal;

    private String note;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderItemOption> options = new ArrayList<>();

    public void addOption(OrderItemOption option) {
        option.setOrderItem(this);
        this.options.add(option);
    }
}
