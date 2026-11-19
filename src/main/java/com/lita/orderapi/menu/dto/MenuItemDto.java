package com.lita.orderapi.menu.dto;

import com.lita.orderapi.menu.MenuItem;
import com.lita.orderapi.option.dto.OptionGroupDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record MenuItemDto(
        UUID id,
        String name,
        String nameTh,
        String description,
        BigDecimal price,
        UUID categoryId,
        String emoji,
        String imageUrl,
        boolean available,
        List<OptionGroupDto> optionGroups
) {
    public static MenuItemDto from(MenuItem menu) {
        return new MenuItemDto(
                menu.getId(),
                menu.getName(),
                menu.getNameTh(),
                menu.getDescription(),
                menu.getPrice(),
                menu.getCategory().getId(),
                menu.getEmoji(),
                menu.getImageUrl(),
                menu.isAvailable(),
                menu.getOptionGroups().stream().map(OptionGroupDto::from).toList()
        );
    }
}
