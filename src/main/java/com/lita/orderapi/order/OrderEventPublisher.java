package com.lita.orderapi.order;

import com.lita.orderapi.order.dto.OrderDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ส่งออเดอร์ใหม่/ออเดอร์ที่เปลี่ยนสถานะไปให้หน้าจอที่เปิดอยู่แบบ realtime
 * ผ่าน Server-Sent Events
 *
 * <p>เลือก SSE แทน WebSocket เพราะทิศทางข้อมูลเป็นทางเดียว (server → browser)
 * วิ่งบน HTTP ปกติ ผ่าน proxy และ Cloud Run ได้โดยไม่ต้องตั้งอะไรเพิ่ม
 *
 * <p>จุดที่ต้องระวัง: ต้องส่ง <b>หลัง transaction commit</b> เท่านั้น
 * ไม่งั้นหน้าจอจะได้ event ตอนที่ข้อมูลยังไม่ลง database จริง
 * แล้วถ้ากดดูทันทีจะหาไม่เจอ
 */
@Slf4j
@Component
public class OrderEventPublisher {

    private static final long TIMEOUT_MILLIS = 30 * 60 * 1000L;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MILLIS);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(throwable -> emitters.remove(emitter));

        emitters.add(emitter);
        sendTo(emitter, "connected", "ok");
        return emitter;
    }

    public void publishOrderCreated(ShopOrder order) {
        afterCommit("order-created", OrderDto.from(order));
    }

    public void publishOrderUpdated(ShopOrder order) {
        afterCommit("order-updated", OrderDto.from(order));
    }

    private void afterCommit(String eventName, Object payload) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            broadcast(eventName, payload);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                broadcast(eventName, payload);
            }
        });
    }

    private void broadcast(String eventName, Object payload) {
        emitters.forEach(emitter -> sendTo(emitter, eventName, payload));
    }

    private void sendTo(SseEmitter emitter, String eventName, Object payload) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(payload));
        } catch (IOException | IllegalStateException ex) {
            // ปลายทางปิดแท็บไปแล้ว เอาออกจากรายการเงียบ ๆ ไม่ใช่เรื่องผิดปกติ
            emitters.remove(emitter);
        }
    }
}
