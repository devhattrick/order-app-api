package com.lita.orderapi.menu.dto;

import com.lita.orderapi.menu.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SaveMenuRequest(
        @NotBlank(message = "กรุณากรอกชื่อเมนู")
        String name,

        String nameTh,

        String description,

        @NotNull(message = "กรุณากรอกราคา")
        @PositiveOrZero(message = "ราคาต้องไม่ติดลบ")
        BigDecimal price,

        @NotNull(message = "กรุณาเลือกหมวดหมู่")
        Category category,

        String emoji,

        String imageUrl,

        Boolean available,

        List<UUID> optionGroupIds
) {
}
