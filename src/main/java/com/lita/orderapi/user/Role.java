package com.lita.orderapi.user;

/**
 * สิทธิ์ของผู้ใช้งานระบบหน้าร้าน
 *
 * <ul>
 *   <li>{@code ADMIN} — เจ้าของร้าน/ผู้จัดการ ทำได้ทุกอย่าง</li>
 *   <li>{@code STAFF} — พนักงานหน้าร้าน ขายของได้ แต่ดู Dashboard
 *       และแก้ไข/ลบเมนูไม่ได้</li>
 * </ul>
 */
public enum Role {
    ADMIN,
    STAFF
}
