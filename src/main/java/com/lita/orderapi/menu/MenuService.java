package com.lita.orderapi.menu;

import com.lita.orderapi.category.MenuCategoryService;
import com.lita.orderapi.common.ApiException;
import com.lita.orderapi.menu.dto.SaveMenuRequest;
import com.lita.orderapi.option.OptionGroup;
import com.lita.orderapi.option.OptionGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menus;
    private final OptionGroupRepository optionGroups;
    private final MenuCategoryService categoryService;

    @Transactional(readOnly = true)
    public List<MenuItem> findAll(UUID categoryId, Boolean availableOnly) {
        List<MenuItem> result = categoryId == null
                ? menus.findAllByOrderBySortOrderAscNameAsc()
                : menus.findAllByCategoryIdOrderBySortOrderAscNameAsc(categoryId);

        if (Boolean.TRUE.equals(availableOnly)) {
            return result.stream().filter(MenuItem::isAvailable).toList();
        }
        return result;
    }

    @Transactional(readOnly = true)
    public MenuItem findById(UUID id) {
        return menus.findById(id)
                .orElseThrow(() -> ApiException.notFound("ไม่พบเมนูที่ต้องการ"));
    }

    @Transactional
    public MenuItem create(SaveMenuRequest request) {
        MenuItem menu = new MenuItem();
        menu.setSortOrder((int) menus.count() + 1);
        applyRequest(menu, request);
        return menus.save(menu);
    }

    @Transactional
    public MenuItem update(UUID id, SaveMenuRequest request) {
        MenuItem menu = findById(id);
        applyRequest(menu, request);
        return menus.save(menu);
    }

    /**
     * เปิด/ปิดขายเมนู
     *
     * ตั้งใจให้ STAFF ทำได้ เพราะเป็นงานหน้างานตอนของหมด
     * ต่างจากการแก้ไข/ลบเมนูที่สงวนไว้ให้ ADMIN เท่านั้น
     */
    @Transactional
    public MenuItem setAvailability(UUID id, boolean available) {
        MenuItem menu = findById(id);
        menu.setAvailable(available);
        return menus.save(menu);
    }

    @Transactional
    public void delete(UUID id) {
        menus.delete(findById(id));
    }

    private void applyRequest(MenuItem menu, SaveMenuRequest request) {
        menu.setName(request.name().trim());
        menu.setNameTh(request.nameTh() == null || request.nameTh().isBlank()
                ? request.name().trim()
                : request.nameTh().trim());
        menu.setDescription(request.description());
        menu.setPrice(request.price());
        menu.setCategory(categoryService.findById(request.categoryId()));
        menu.setEmoji(request.emoji());
        menu.setImageUrl(request.imageUrl());
        menu.setAvailable(request.available() == null || request.available());

        List<UUID> groupIds = request.optionGroupIds() == null ? List.of() : request.optionGroupIds();
        List<OptionGroup> groups = optionGroups.findAllById(groupIds);

        if (groups.size() != groupIds.size()) {
            throw ApiException.badRequest("มีกลุ่มตัวเลือกบางรายการที่ไม่มีอยู่ในระบบแล้ว");
        }
        menu.setOptionGroups(groups);
    }
}
