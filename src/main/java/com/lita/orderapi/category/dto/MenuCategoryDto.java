package com.lita.orderapi.category.dto;

import com.lita.orderapi.category.MenuCategory;

import java.util.UUID;

public record MenuCategoryDto(
        UUID id,
        String name,
        String nameEn,
        int sortOrder,
        /** จำนวนเมนูที่ใช้หมวดนี้อยู่ — หน้าจอใช้เตือนก่อนลบ */
        long menuCount
) {
    public static MenuCategoryDto from(MenuCategory category, long menuCount) {
        return new MenuCategoryDto(
                category.getId(),
                category.getName(),
                category.getNameEn(),
                category.getSortOrder(),
                menuCount
        );
    }
}
