package com.lita.orderapi.option.dto;

import com.lita.orderapi.option.OptionGroup;
import com.lita.orderapi.option.OptionGroupType;

import java.util.List;
import java.util.UUID;

public record OptionGroupDto(
        UUID id,
        String name,
        OptionGroupType type,
        boolean required,
        UUID defaultChoiceId,
        List<OptionChoiceDto> choices
) {
    public static OptionGroupDto from(OptionGroup group) {
        return new OptionGroupDto(
                group.getId(),
                group.getName(),
                group.getType(),
                group.isRequired(),
                group.getDefaultChoiceId(),
                group.getChoices().stream().map(OptionChoiceDto::from).toList()
        );
    }
}
