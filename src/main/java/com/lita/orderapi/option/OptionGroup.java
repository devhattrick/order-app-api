package com.lita.orderapi.option;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * กลุ่มตัวเลือกเสริม เช่น "ขนาด" หรือ "ระดับความหวาน"
 *
 * ออกแบบเป็น master data แยกจากเมนู แล้วผูกกันแบบ many-to-many
 * เพื่อให้แก้ที่เดียวแล้วมีผลกับทุกเมนูที่ใช้กลุ่มนี้
 */
@Entity
@Table(name = "option_group")
@Getter
@Setter
@NoArgsConstructor
public class OptionGroup {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OptionGroupType type = OptionGroupType.SINGLE;

    /** true = ลูกค้าต้องเลือกก่อนใส่ตะกร้า */
    @Column(nullable = false)
    private boolean required;

    @Column(name = "default_choice_id")
    private UUID defaultChoiceId;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("sortOrder asc")
    private List<OptionChoice> choices = new ArrayList<>();

    /** ใส่ตัวเลือกย่อยพร้อมผูกความสัมพันธ์สองทางให้เรียบร้อย */
    public void replaceChoices(List<OptionChoice> newChoices) {
        this.choices.clear();
        newChoices.forEach(choice -> {
            choice.setGroup(this);
            this.choices.add(choice);
        });
    }

    public OptionChoice findChoice(UUID choiceId) {
        return choices.stream()
                .filter(choice -> choice.getId().equals(choiceId))
                .findFirst()
                .orElse(null);
    }
}
