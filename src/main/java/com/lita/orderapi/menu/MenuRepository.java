package com.lita.orderapi.menu;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MenuRepository extends JpaRepository<MenuItem, UUID> {

    List<MenuItem> findAllByOrderBySortOrderAscNameAsc();

    List<MenuItem> findAllByCategoryIdOrderBySortOrderAscNameAsc(UUID categoryId);

    /** ใช้ตอนเช็คว่าหมวดหมู่ยังมีเมนูใช้อยู่ไหม ก่อนอนุญาตให้ลบ */
    long countByCategoryId(UUID categoryId);
}
