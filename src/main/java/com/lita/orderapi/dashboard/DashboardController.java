package com.lita.orderapi.dashboard;

import com.lita.orderapi.dashboard.dto.DashboardSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * รายงานยอดขาย — ADMIN เท่านั้น
 *
 * STAFF ที่เรียก endpoint นี้จะได้ 403 กลับไป (กติกาอยู่ที่ SecurityConfig)
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public DashboardSummary summary(@RequestParam(defaultValue = "14") int days) {
        return dashboardService.getSummary(Math.max(1, Math.min(days, 90)));
    }
}
