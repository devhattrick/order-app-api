package com.lita.orderapi.dashboard.dto;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** ข้อมูลทั้งหน้า Dashboard รวมใน response เดียว — frontend ยิงครั้งเดียวจบ */
public record DashboardSummary(
        BigDecimal revenueToday,
        long ordersToday,
        BigDecimal averagePerOrderToday,
        int dineInSharePercent,
        long availableMenuCount,
        List<DailySales> dailySales,
        List<HourlyOrders> hourlyOrders,
        List<CategorySales> salesByCategory,
        List<TopMenu> topMenus
) {

    public record DailySales(LocalDate date, BigDecimal revenue, long orders) {
    }

    public record HourlyOrders(int hour, double averageOrders) {
    }

    public record CategorySales(String category, BigDecimal revenue) {
    }

    public record TopMenu(String menuName, long qty, BigDecimal revenue) {
    }
}
