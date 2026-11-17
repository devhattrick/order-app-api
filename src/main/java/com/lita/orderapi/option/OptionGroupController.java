package com.lita.orderapi.option;

import com.lita.orderapi.option.dto.OptionGroupDto;
import com.lita.orderapi.option.dto.SaveOptionGroupRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * จัดการกลุ่มตัวเลือกเสริม
 *
 * อ่านได้ทุกคนที่ login แล้ว · เพิ่ม/แก้/ลบ เฉพาะ ADMIN (ดู SecurityConfig)
 */
@RestController
@RequestMapping("/api/option-groups")
@RequiredArgsConstructor
public class OptionGroupController {

    private final OptionGroupService optionGroupService;

    @GetMapping
    public List<OptionGroupDto> list() {
        return optionGroupService.findAll().stream().map(OptionGroupDto::from).toList();
    }

    @GetMapping("/{id}/usage")
    public Map<String, Long> usage(@PathVariable UUID id) {
        return Map.of("menuCount", optionGroupService.countMenusUsing(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OptionGroupDto create(@Valid @RequestBody SaveOptionGroupRequest request) {
        return OptionGroupDto.from(optionGroupService.create(request));
    }

    @PutMapping("/{id}")
    public OptionGroupDto update(@PathVariable UUID id, @Valid @RequestBody SaveOptionGroupRequest request) {
        return OptionGroupDto.from(optionGroupService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        optionGroupService.delete(id);
    }
}
