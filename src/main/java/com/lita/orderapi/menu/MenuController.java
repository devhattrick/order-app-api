package com.lita.orderapi.menu;

import com.lita.orderapi.menu.dto.MenuItemDto;
import com.lita.orderapi.menu.dto.SaveMenuRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * จัดการเมนู
 *
 * <ul>
 *   <li>อ่าน — ทุกคนที่ login แล้ว</li>
 *   <li>เพิ่ม/แก้ไข/ลบ — ADMIN เท่านั้น (บังคับที่ SecurityConfig)</li>
 *   <li>เปิด/ปิดขาย — STAFF ทำได้ เพราะเป็นงานหน้างานตอนของหมด</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @GetMapping
    public List<MenuItemDto> list(@RequestParam(required = false) UUID categoryId,
                                  @RequestParam(required = false) Boolean availableOnly) {
        return menuService.findAll(categoryId, availableOnly).stream().map(MenuItemDto::from).toList();
    }

    @GetMapping("/{id}")
    public MenuItemDto getOne(@PathVariable UUID id) {
        return MenuItemDto.from(menuService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MenuItemDto create(@Valid @RequestBody SaveMenuRequest request) {
        return MenuItemDto.from(menuService.create(request));
    }

    @PutMapping("/{id}")
    public MenuItemDto update(@PathVariable UUID id, @Valid @RequestBody SaveMenuRequest request) {
        return MenuItemDto.from(menuService.update(id, request));
    }

    @PatchMapping("/{id}/availability")
    public MenuItemDto setAvailability(@PathVariable UUID id, @RequestBody Map<String, Boolean> body) {
        boolean available = Boolean.TRUE.equals(body.get("available"));
        return MenuItemDto.from(menuService.setAvailability(id, available));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        menuService.delete(id);
    }
}
