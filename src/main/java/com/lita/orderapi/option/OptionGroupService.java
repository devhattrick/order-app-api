package com.lita.orderapi.option;

import com.lita.orderapi.common.ApiException;
import com.lita.orderapi.menu.MenuItem;
import com.lita.orderapi.menu.MenuRepository;
import com.lita.orderapi.option.dto.SaveOptionGroupRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OptionGroupService {

    private final OptionGroupRepository optionGroups;
    private final MenuRepository menus;

    @Transactional(readOnly = true)
    public List<OptionGroup> findAll() {
        return optionGroups.findAllByOrderBySortOrderAscNameAsc();
    }

    @Transactional(readOnly = true)
    public OptionGroup findById(UUID id) {
        return optionGroups.findById(id)
                .orElseThrow(() -> ApiException.notFound("ไม่พบกลุ่มตัวเลือกที่ต้องการ"));
    }

    @Transactional
    public OptionGroup create(SaveOptionGroupRequest request) {
        OptionGroup group = new OptionGroup();
        group.setSortOrder(optionGroups.count() > 0 ? (int) optionGroups.count() + 1 : 1);
        applyRequest(group, request);
        return optionGroups.save(group);
    }

    @Transactional
    public OptionGroup update(UUID id, SaveOptionGroupRequest request) {
        OptionGroup group = findById(id);
        applyRequest(group, request);
        return optionGroups.save(group);
    }

    /**
     * ลบกลุ่มตัวเลือก พร้อมถอดออกจากทุกเมนูที่ผูกอยู่
     *
     * ออเดอร์เก่าไม่กระทบ เพราะเก็บชื่อและราคาตัวเลือกเป็น snapshot ไว้แล้ว
     */
    @Transactional
    public void delete(UUID id) {
        OptionGroup group = findById(id);

        List<MenuItem> affected = menus.findAll().stream()
                .filter(menu -> menu.getOptionGroups().removeIf(g -> g.getId().equals(id)))
                .toList();
        menus.saveAll(affected);

        optionGroups.delete(group);
    }

    /** นับว่ามีกี่เมนูที่ใช้กลุ่มนี้อยู่ — frontend เอาไปเตือนก่อนแก้/ลบ */
    @Transactional(readOnly = true)
    public long countMenusUsing(UUID groupId) {
        return menus.findAll().stream()
                .filter(menu -> menu.getOptionGroups().stream().anyMatch(g -> g.getId().equals(groupId)))
                .count();
    }

    private void applyRequest(OptionGroup group, SaveOptionGroupRequest request) {
        group.setName(request.name().trim());
        group.setType(request.type());
        group.setRequired(request.required());

        List<OptionChoice> choices = request.choices().stream().map(input -> {
            OptionChoice choice = new OptionChoice();
            if (input.id() != null) {
                choice.setId(input.id());
            }
            choice.setName(input.name().trim());
            choice.setPriceDelta(input.priceDelta() == null ? BigDecimal.ZERO : input.priceDelta());
            return choice;
        }).toList();

        for (int i = 0; i < choices.size(); i++) {
            choices.get(i).setSortOrder(i);
        }
        group.replaceChoices(choices);

        // ค่าเริ่มต้นต้องเป็นตัวเลือกที่ยังอยู่ในกลุ่มจริง ๆ เท่านั้น
        boolean defaultStillExists = request.defaultChoiceId() != null
                && choices.stream().anyMatch(choice -> choice.getId().equals(request.defaultChoiceId()));
        group.setDefaultChoiceId(defaultStillExists ? request.defaultChoiceId() : null);
    }
}
