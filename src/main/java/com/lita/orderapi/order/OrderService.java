package com.lita.orderapi.order;

import com.lita.orderapi.common.ApiException;
import com.lita.orderapi.menu.MenuItem;
import com.lita.orderapi.menu.MenuService;
import com.lita.orderapi.option.OptionChoice;
import com.lita.orderapi.option.OptionGroup;
import com.lita.orderapi.option.OptionGroupType;
import com.lita.orderapi.order.dto.CreateOrderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final String TOKEN_ALPHABET = "abcdefghijkmnpqrstuvwxyz23456789";
    private static final int TOKEN_LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrderRepository orders;
    private final MenuService menuService;
    private final DocumentNumberService documentNumbers;

    // ---------------------------------------------------------------
    // สร้างออเดอร์
    // ---------------------------------------------------------------

    @Transactional
    public ShopOrder create(CreateOrderRequest request, String username) {
        validateOrderTypeAndTable(request);

        ShopOrder order = new ShopOrder();
        order.setOrderType(request.orderType());
        order.setTableNo(request.orderType() == OrderType.DINE_IN ? request.tableNo().trim() : null);
        order.setSource(OrderSource.POS);
        order.setNote(blankToNull(request.note()));
        order.setTrackingToken(generateTrackingToken());

        LocalDate businessDate = DocumentNumberService.today();
        order.setOrderNo(documentNumbers.nextOrderNo(businessDate));
        order.setQueueNo(documentNumbers.nextQueueNo(businessDate, request.orderType()));

        BigDecimal total = BigDecimal.ZERO;
        for (CreateOrderRequest.ItemInput input : request.items()) {
            OrderItem item = buildItem(input);
            order.addItem(item);
            total = total.add(item.getLineTotal());
        }
        order.setTotalAmount(total);

        // ซื้อกลับ = รับเงินตอนสั่งที่เคาน์เตอร์ก่อนเข้าครัว / ทานที่ร้าน = เก็บตอนเช็คบิล
        if (request.orderType() == OrderType.TAKEAWAY) {
            order.setPaidAt(Instant.now());
        }

        order.changeStatus(OrderStatus.PENDING, username);

        return orders.save(order);
    }

    /**
     * แปลง 1 รายการที่ส่งมาเป็น OrderItem พร้อมคำนวณราคา
     *
     * ราคาทุกบาทมาจาก database ไม่ได้มาจาก request — ถ้าเชื่อราคาที่ client ส่งมา
     * ใครก็ยิง API สั่งขนมปัง 220 บาทในราคา 1 บาทได้
     */
    private OrderItem buildItem(CreateOrderRequest.ItemInput input) {
        MenuItem menu = menuService.findById(input.menuId());

        if (!menu.isAvailable()) {
            throw ApiException.conflict("เมนู \"%s\" ปิดขายอยู่".formatted(menu.getName()));
        }

        OrderItem item = new OrderItem();
        item.setMenuId(menu.getId());
        item.setMenuName(menu.getName());
        item.setCategory(menu.getCategory());
        item.setEmoji(menu.getEmoji());
        item.setQty(input.qty());
        item.setNote(blankToNull(input.note()));

        BigDecimal unitPrice = menu.getPrice();
        for (OrderItemOption option : resolveOptions(menu, input.options())) {
            item.addOption(option);
            unitPrice = unitPrice.add(option.getPriceDelta());
        }

        item.setUnitPrice(unitPrice);
        item.setLineTotal(unitPrice.multiply(BigDecimal.valueOf(input.qty())));
        return item;
    }

    /** ตรวจว่าตัวเลือกที่เลือกมาใช้กับเมนูนี้ได้จริง แล้วแปลงเป็น snapshot */
    private List<OrderItemOption> resolveOptions(MenuItem menu,
                                                 List<CreateOrderRequest.SelectedOptionInput> selections) {

        List<CreateOrderRequest.SelectedOptionInput> inputs =
                selections == null ? List.of() : selections;

        Map<UUID, OptionGroup> allowedGroups = new LinkedHashMap<>();
        menu.getOptionGroups().forEach(group -> allowedGroups.put(group.getId(), group));

        List<OrderItemOption> result = new ArrayList<>();
        Map<UUID, Integer> pickedPerGroup = new HashMap<>();

        for (CreateOrderRequest.SelectedOptionInput selection : inputs) {
            OptionGroup group = allowedGroups.get(selection.groupId());
            if (group == null) {
                throw ApiException.badRequest(
                        "เมนู \"%s\" ไม่มีกลุ่มตัวเลือกที่เลือกมา".formatted(menu.getName()));
            }

            OptionChoice choice = group.findChoice(selection.choiceId());
            if (choice == null) {
                throw ApiException.badRequest(
                        "ไม่พบตัวเลือกที่เลือกในกลุ่ม \"%s\"".formatted(group.getName()));
            }

            int picked = pickedPerGroup.merge(group.getId(), 1, Integer::sum);
            if (group.getType() == OptionGroupType.SINGLE && picked > 1) {
                throw ApiException.badRequest(
                        "กลุ่ม \"%s\" เลือกได้แค่อย่างเดียว".formatted(group.getName()));
            }

            OrderItemOption option = new OrderItemOption();
            option.setGroupId(group.getId());
            option.setGroupName(group.getName());
            option.setChoiceId(choice.getId());
            option.setChoiceName(choice.getName());
            option.setPriceDelta(choice.getPriceDelta());
            result.add(option);
        }

        // กลุ่มที่บังคับเลือก ต้องมีอย่างน้อย 1 ตัวเลือก
        allowedGroups.values().stream()
                .filter(OptionGroup::isRequired)
                .filter(group -> !pickedPerGroup.containsKey(group.getId()))
                .findFirst()
                .ifPresent(group -> {
                    throw ApiException.badRequest(
                            "กรุณาเลือก \"%s\" สำหรับเมนู \"%s\"".formatted(group.getName(), menu.getName()));
                });

        return result;
    }

    private void validateOrderTypeAndTable(CreateOrderRequest request) {
        boolean hasTable = request.tableNo() != null && !request.tableNo().isBlank();

        if (request.orderType() == OrderType.DINE_IN && !hasTable) {
            throw ApiException.badRequest("ออเดอร์ทานที่ร้านต้องระบุโต๊ะ");
        }
        if (request.orderType() == OrderType.TAKEAWAY && hasTable) {
            throw ApiException.badRequest("ออเดอร์ซื้อกลับไม่ต้องระบุโต๊ะ");
        }
    }

    // ---------------------------------------------------------------
    // เปลี่ยนสถานะ
    // ---------------------------------------------------------------

    @Transactional
    public ShopOrder changeStatus(UUID orderId, OrderStatus target, String username) {
        ShopOrder order = findById(orderId);
        OrderStatus current = order.getStatus();

        if (current.isTerminal()) {
            throw ApiException.conflict("ออเดอร์นี้จบไปแล้ว เปลี่ยนสถานะไม่ได้");
        }

        if (target == OrderStatus.CANCELLED) {
            order.changeStatus(OrderStatus.CANCELLED, username);
            return orders.save(order);
        }

        OrderStatus allowedNext = current.next(order.getOrderType());
        if (allowedNext == null) {
            throw ApiException.conflict(
                    "ออเดอร์ที่สถานะ \"%s\" ต้องปิดบิลที่ผังโต๊ะเท่านั้น".formatted(current));
        }
        if (target != allowedNext) {
            throw ApiException.conflict(
                    "เปลี่ยนสถานะจาก %s ไป %s ไม่ได้ ขั้นถัดไปคือ %s".formatted(current, target, allowedNext));
        }

        order.changeStatus(target, username);
        if (target == OrderStatus.COMPLETED && !order.isPaid()) {
            order.setPaidAt(Instant.now());
        }

        return orders.save(order);
    }

    // ---------------------------------------------------------------
    // อ่านข้อมูล
    // ---------------------------------------------------------------

    @Transactional(readOnly = true)
    public ShopOrder findById(UUID id) {
        return orders.findById(id)
                .orElseThrow(() -> ApiException.notFound("ไม่พบออเดอร์ที่ต้องการ"));
    }

    @Transactional(readOnly = true)
    public ShopOrder findByTrackingToken(String token) {
        return orders.findByTrackingToken(token)
                .orElseThrow(() -> ApiException.notFound("ไม่พบออเดอร์นี้ หรือลิงก์อาจหมดอายุแล้ว"));
    }

    /** ออเดอร์ที่ยังไม่จบ — ใช้แสดงบนกระดานครัว */
    @Transactional(readOnly = true)
    public List<ShopOrder> findActive() {
        return orders.findByStatusInOrderByCreatedAtAsc(OrderStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public Page<ShopOrder> search(Instant from, Instant to, OrderStatus status,
                                  OrderType orderType, String keyword, Pageable pageable) {
        return orders.findAll(OrderSpecifications.filter(from, to, status, orderType, keyword), pageable);
    }

    private String generateTrackingToken() {
        StringBuilder token = new StringBuilder(TOKEN_LENGTH);
        for (int i = 0; i < TOKEN_LENGTH; i++) {
            token.append(TOKEN_ALPHABET.charAt(RANDOM.nextInt(TOKEN_ALPHABET.length())));
        }
        return token.toString();
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
