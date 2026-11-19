package com.lita.orderapi.category;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * หมวดหมู่เมนู — ร้านเพิ่ม/แก้/ลบเองได้จากหน้าจัดการเมนู
 *
 * <p>เดิมเป็น enum ในโค้ด ทุกครั้งที่ร้านอยากเพิ่มหมวดต้องแก้โค้ดและ deploy ใหม่
 * ซึ่งไม่เหมาะกับของที่เปลี่ยนตามฤดูกาลหรือโปรโมชั่น
 */
@Entity
@Table(name = "menu_category")
@Getter
@Setter
@NoArgsConstructor
public class MenuCategory {

    @Id
    private UUID id = UUID.randomUUID();

    /** ชื่อที่แสดงบนหน้าจอ เช่น "เครื่องดื่ม" — ห้ามซ้ำ */
    @Column(nullable = false, unique = true)
    private String name;

    /** ชื่อภาษาอังกฤษ ไม่บังคับ */
    @Column(name = "name_en")
    private String nameEn;

    /** ลำดับการแสดงบนหน้าจอ เลขน้อยอยู่ก่อน */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }
}
