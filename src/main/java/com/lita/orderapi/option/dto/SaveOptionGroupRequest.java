package com.lita.orderapi.option.dto;

import com.lita.orderapi.option.OptionGroupType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SaveOptionGroupRequest(
        @NotBlank(message = "กรุณากรอกชื่อกลุ่ม")
        String name,

        @NotNull(message = "กรุณาเลือกรูปแบบการเลือก")
        OptionGroupType type,

        boolean required,

        UUID defaultChoiceId,

        @NotEmpty(message = "ต้องมีตัวเลือกอย่างน้อย 1 รายการ")
        @Valid
        List<ChoiceInput> choices
) {
    public record ChoiceInput(
            UUID id,

            @NotBlank(message = "กรุณากรอกชื่อตัวเลือก")
            String name,

            BigDecimal priceDelta
    ) {
    }
}
