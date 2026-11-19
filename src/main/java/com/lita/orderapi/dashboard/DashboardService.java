package com.lita.orderapi.dashboard;

import com.lita.orderapi.dashboard.dto.DashboardSummary;
import com.lita.orderapi.menu.MenuRepository;
import com.lita.orderapi.order.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * คำนวณตัวเลขหน้า Dashboard
 *
 * <p>ตอนนี้ดึงออเดอร์ในช่วงที่สนใจมาคำนวณใน memory ซึ่งเร็วพอที่ระดับ
 * ร้านเดียว (หลักพันออเดอร์ต่อเดือน) แต่ถ้าขยายหลายสาขาเมื่อไหร่
 * ควรเปลี่ยนไปอ่านจากตารางสรุปรายวันที่ทำไว้ล่วงหน้าแทน
 * ไม่อย่างนั้นทุกครั้งที่เปิดหน้านี้จะไปกวาดตารางออเดอร์ทั้งก้อน
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int OPEN_HOUR = 8;
    private static final int CLOSE_HOUR = 18;

    private final OrderRepository orders;
    private final MenuRepository menus;

    @Transactional(readOnly = true)
    public DashboardSummary getSummary(int days) {
        LocalDate today = DocumentNumberService.today();
        LocalDate startDate = today.minusDays(days - 1L);

        Instant from = startDate.atStartOfDay(DocumentNumberService.SHOP_ZONE).toInstant();
        Instant to = today.plusDays(1).atStartOfDay(DocumentNumberService.SHOP_ZONE).toInstant();

        // ออเดอร์ที่ถูกยกเลิกไม่นับเป็นยอดขายในทุกตัวเลข
        List<ShopOrder> paidOrders = orders.findByCreatedAtBetweenOrderByCreatedAtAsc(from, to).stream()
                .filter(order -> order.getStatus() != OrderStatus.CANCELLED)
                .toList();

        List<ShopOrder> todayOrders = paidOrders.stream()
                .filter(order -> businessDateOf(order).equals(today))
                .toList();

        BigDecimal revenueToday = sumTotal(todayOrders);
        BigDecimal averageToday = todayOrders.isEmpty()
                ? BigDecimal.ZERO
                : revenueToday.divide(BigDecimal.valueOf(todayOrders.size()), 0, RoundingMode.HALF_UP);

        long dineInCount = paidOrders.stream()
                .filter(order -> order.getOrderType() == OrderType.DINE_IN)
                .count();
        int dineInShare = paidOrders.isEmpty()
                ? 0
                : Math.round((float) dineInCount * 100 / paidOrders.size());

        return new DashboardSummary(
                revenueToday,
                todayOrders.size(),
                averageToday,
                dineInShare,
                menus.findAll().stream().filter(menu -> menu.isAvailable()).count(),
                buildDailySales(paidOrders, startDate, today),
                buildHourlyOrders(paidOrders, days),
                buildCategorySales(paidOrders),
                buildTopMenus(paidOrders)
        );
    }

    private List<DashboardSummary.DailySales> buildDailySales(List<ShopOrder> source,
                                                              LocalDate startDate,
                                                              LocalDate endDate) {
        Map<LocalDate, List<ShopOrder>> grouped = source.stream()
                .collect(Collectors.groupingBy(this::businessDateOf));

        List<DashboardSummary.DailySales> result = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            List<ShopOrder> ofDay = grouped.getOrDefault(date, List.of());
            result.add(new DashboardSummary.DailySales(date, sumTotal(ofDay), ofDay.size()));
        }
        return result;
    }

    private List<DashboardSummary.HourlyOrders> buildHourlyOrders(List<ShopOrder> source, int days) {
        Map<Integer, Long> countByHour = source.stream()
                .collect(Collectors.groupingBy(
                        order -> order.getCreatedAt().atZone(DocumentNumberService.SHOP_ZONE).getHour(),
                        Collectors.counting()));

        List<DashboardSummary.HourlyOrders> result = new ArrayList<>();
        for (int hour = OPEN_HOUR; hour <= CLOSE_HOUR; hour++) {
            long count = countByHour.getOrDefault(hour, 0L);
            double average = Math.round((double) count / days * 10) / 10.0;
            result.add(new DashboardSummary.HourlyOrders(hour, average));
        }
        return result;
    }

    private List<DashboardSummary.CategorySales> buildCategorySales(List<ShopOrder> source) {
        // ใช้ชื่อหมวดที่เก็บไว้ใน order_item ไม่ใช่หมวดปัจจุบัน
        // รายงานย้อนหลังจึงคงเดิมแม้ร้านจะเปลี่ยนชื่อหรือลบหมวดไปแล้ว
        Map<String, BigDecimal> byCategory = new java.util.HashMap<>();

        source.stream()
                .flatMap(order -> order.getItems().stream())
                .forEach(item -> byCategory.merge(item.getCategory(), item.getLineTotal(), BigDecimal::add));

        return byCategory.entrySet().stream()
                .map(entry -> new DashboardSummary.CategorySales(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(DashboardSummary.CategorySales::revenue).reversed())
                .toList();
    }

    private List<DashboardSummary.TopMenu> buildTopMenus(List<ShopOrder> source) {
        record Tally(long qty, BigDecimal revenue) {
        }

        Map<String, Tally> byMenu = new LinkedHashMap<>();
        source.stream()
                .flatMap(order -> order.getItems().stream())
                .forEach(item -> byMenu.merge(
                        item.getMenuName(),
                        new Tally(item.getQty(), item.getLineTotal()),
                        (a, b) -> new Tally(a.qty() + b.qty(), a.revenue().add(b.revenue()))));

        return byMenu.entrySet().stream()
                .map(entry -> new DashboardSummary.TopMenu(
                        entry.getKey(), entry.getValue().qty(), entry.getValue().revenue()))
                .sorted(Comparator.comparingLong(DashboardSummary.TopMenu::qty).reversed())
                .limit(6)
                .toList();
    }

    private BigDecimal sumTotal(List<ShopOrder> source) {
        return source.stream()
                .map(ShopOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private LocalDate businessDateOf(ShopOrder order) {
        return order.getCreatedAt().atZone(DocumentNumberService.SHOP_ZONE).toLocalDate();
    }
}
