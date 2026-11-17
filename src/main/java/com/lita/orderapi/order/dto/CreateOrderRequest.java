package com.lita.orderapi.order.dto;

import com.lita.orderapi.order.OrderType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * ข้อมูลที่หน้าร้านส่งมาตอนกด "ส่งเข้าครัว"
 *
 * สังเกตว่า <b>ไม่มีราคาอยู่ใน request เลย</b> — ฝั่ง server คำนวณเองทั้งหมด
 * จากราคาเมนูและตัวเลือกใน database เพื่อไม่ให้ใครแก้ราคาผ่าน API ได้
 */
public record CreateOrderRequest(

        @NotNull(message = "กรุณาระบุประเภทออเดอร์")
        OrderType orderType,

        /** ต้องมีค่าเมื่อเป็น DINE_IN เท่านั้น */
        String tableNo,

        String note,

        @NotEmpty(message = "ต้องมีรายการอาหารอย่างน้อย 1 รายการ")
        @Valid
        List<ItemInput> items
) {

    public record ItemInput(
            @NotNull(message = "กรุณาระบุเมนู")
            UUID menuId,

            @Min(value = 1, message = "จำนวนต้องอย่างน้อย 1")
            int qty,

            String note,

            @Valid
            List<SelectedOptionInput> options
    ) {
    }

    public record SelectedOptionInput(
            @NotNull UUID groupId,
            @NotNull UUID choiceId
    ) {
    }
}
