package com.lita.orderapi.category;

import com.lita.orderapi.category.dto.MenuCategoryDto;
import com.lita.orderapi.category.dto.SaveCategoryRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * จัดการหมวดหมู่เมนู
 *
 * <ul>
 *   <li>อ่าน — ทุกคนที่ login แล้ว (STAFF ต้องเห็นหมวดเพื่อกรองเมนูหน้าร้าน)</li>
 *   <li>เพิ่ม/แก้ไข/ลบ — ADMIN เท่านั้น (บังคับที่ SecurityConfig)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class MenuCategoryController {

    private final MenuCategoryService categoryService;

    @GetMapping
    public List<MenuCategoryDto> list() {
        return categoryService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MenuCategoryDto create(@Valid @RequestBody SaveCategoryRequest request) {
        return categoryService.create(request);
    }

    @PutMapping("/{id}")
    public MenuCategoryDto update(@PathVariable UUID id, @Valid @RequestBody SaveCategoryRequest request) {
        return categoryService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        categoryService.delete(id);
    }
}
