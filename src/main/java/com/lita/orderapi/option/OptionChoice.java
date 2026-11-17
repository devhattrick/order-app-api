package com.lita.orderapi.option;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "option_choice")
@Getter
@Setter
@NoArgsConstructor
public class OptionChoice {

    @Id
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_group_id", nullable = false)
    private OptionGroup group;

    @Column(nullable = false)
    private String name;

    /** ราคาที่บวกเพิ่มจากราคาเมนู เช่น Large +20 */
    @Column(name = "price_delta", nullable = false)
    private BigDecimal priceDelta = BigDecimal.ZERO;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
