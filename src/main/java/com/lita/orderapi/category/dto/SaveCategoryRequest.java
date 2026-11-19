package com.lita.orderapi.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveCategoryRequest(
        @NotBlank(message = "กรุณากรอกชื่อหมวดหมู่")
        @Size(max = 60, message = "ชื่อหมวดหมู่ยาวเกิน 60 ตัวอักษร")
        String name,

        @Size(max = 60, message = "ชื่อภาษาอังกฤษยาวเกิน 60 ตัวอักษร")
        String nameEn,

        Integer sortOrder
) {
}
