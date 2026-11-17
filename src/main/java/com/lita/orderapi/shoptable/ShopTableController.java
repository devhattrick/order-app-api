package com.lita.orderapi.shoptable;

import com.lita.orderapi.order.dto.OrderDto;
import com.lita.orderapi.shoptable.dto.TableStateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/tables")
@RequiredArgsConstructor
public class ShopTableController {

    private final ShopTableService shopTableService;

    /** ผังโต๊ะทั้งหมดพร้อมสถานะ — หน้าหลักของพนักงาน */
    @GetMapping
    public List<TableStateDto> list() {
        return shopTableService.getAllTableStates();
    }

    @PostMapping("/{tableNo}/close")
    public List<OrderDto> close(@PathVariable String tableNo, Principal principal) {
        return shopTableService.closeTable(tableNo, principal.getName()).stream()
                .map(OrderDto::from)
                .toList();
    }
}
