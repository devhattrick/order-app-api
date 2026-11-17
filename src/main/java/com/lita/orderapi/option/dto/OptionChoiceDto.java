package com.lita.orderapi.option.dto;

import com.lita.orderapi.option.OptionChoice;

import java.math.BigDecimal;
import java.util.UUID;

public record OptionChoiceDto(
        UUID id,
        String name,
        BigDecimal priceDelta
) {
    public static OptionChoiceDto from(OptionChoice choice) {
        return new OptionChoiceDto(choice.getId(), choice.getName(), choice.getPriceDelta());
    }
}
