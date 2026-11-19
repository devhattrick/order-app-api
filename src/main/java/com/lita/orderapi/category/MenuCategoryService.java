package com.lita.orderapi.category;

import com.lita.orderapi.category.dto.MenuCategoryDto;
import com.lita.orderapi.category.dto.SaveCategoryRequest;
import com.lita.orderapi.common.ApiException;
import com.lita.orderapi.menu.MenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MenuCategoryService {

    private final MenuCategoryRepository categories;
    private final MenuRepository menus;

    @Transactional(readOnly = true)
    public List<MenuCategoryDto> findAll() {
        return categories.findAllByOrderBySortOrderAscNameAsc().stream()
                .map(category -> MenuCategoryDto.from(category, menus.countByCategoryId(category.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public MenuCategory findById(UUID id) {
        return categories.findById(id)
                .orElseThrow(() -> ApiException.notFound("ไม่พบหมวดหมู่ที่ต้องการ"));
    }

    @Transactional
    public MenuCategoryDto create(SaveCategoryRequest request) {
        MenuCategory category = new MenuCategory();
        // หมวดใหม่ไปต่อท้ายเสมอ ถ้าไม่ได้ระบุลำดับมา
        category.setSortOrder(request.sortOrder() != null
                ? request.sortOrder()
                : (int) categories.count() + 1);
        apply(category, request, null);
        return MenuCategoryDto.from(categories.save(category), 0);
    }

    @Transactional
    public MenuCategoryDto update(UUID id, SaveCategoryRequest request) {
        MenuCategory category = findById(id);
        if (request.sortOrder() != null) {
            category.setSortOrder(request.sortOrder());
        }
        apply(category, request, id);
        return MenuCategoryDto.from(categories.save(category), menus.countByCategoryId(id));
    }

    /**
     * ลบหมวดหมู่
     *
     * <p>ลบไม่ได้ถ้ายังมีเมนูใช้อยู่ เพราะ menu_item.category_id เป็น not null
     * ถ้าปล่อยให้ลบ เมนูเหล่านั้นจะกลายเป็นข้อมูลกำพร้าและหน้าจอจะพัง
     * จึงบอกจำนวนเมนูกลับไปให้ผู้ใช้ย้ายหมวดก่อน
     *
     * <p>ออเดอร์เก่าไม่กระทบ เพราะ order_item เก็บชื่อหมวดไว้เป็น snapshot ของตัวเอง
     */
    @Transactional
    public void delete(UUID id) {
        MenuCategory category = findById(id);

        long inUse = menus.countByCategoryId(id);
        if (inUse > 0) {
            throw ApiException.conflict(
                    "ลบไม่ได้ เพราะยังมี %d เมนูอยู่ในหมวด \"%s\" — ย้ายเมนูไปหมวดอื่นก่อน"
                            .formatted(inUse, category.getName()));
        }

        if (categories.count() <= 1) {
            throw ApiException.conflict("ต้องมีหมวดหมู่อย่างน้อย 1 หมวดในระบบ");
        }

        categories.delete(category);
    }

    private void apply(MenuCategory category, SaveCategoryRequest request, UUID selfId) {
        String name = request.name().trim();

        // ชื่อซ้ำทำให้พนักงานเลือกผิดหมวดได้ง่าย จึงกันไว้ตั้งแต่ต้น
        Optional<MenuCategory> existing = categories.findByNameIgnoreCase(name);
        if (existing.isPresent() && !existing.get().getId().equals(selfId)) {
            throw ApiException.conflict("มีหมวดหมู่ชื่อ \"%s\" อยู่แล้ว".formatted(name));
        }

        category.setName(name);
        category.setNameEn(request.nameEn() == null || request.nameEn().isBlank()
                ? null
                : request.nameEn().trim());
    }
}
