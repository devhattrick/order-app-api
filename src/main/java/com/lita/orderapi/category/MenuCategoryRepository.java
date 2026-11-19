package com.lita.orderapi.category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, UUID> {

    java.util.List<MenuCategory> findAllByOrderBySortOrderAscNameAsc();

    Optional<MenuCategory> findByNameIgnoreCase(String name);
}
