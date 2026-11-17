package com.lita.orderapi.menu;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MenuRepository extends JpaRepository<MenuItem, UUID> {

    List<MenuItem> findAllByOrderBySortOrderAscNameAsc();

    List<MenuItem> findAllByCategoryOrderBySortOrderAscNameAsc(Category category);
}
