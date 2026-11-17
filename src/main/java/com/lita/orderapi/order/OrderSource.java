package com.lita.orderapi.order;

/** ออเดอร์นี้ถูกสร้างจากช่องทางไหน */
public enum OrderSource {
    /** พนักงานคีย์ให้จากหน้าร้าน */
    POS,
    /** ลูกค้าสั่งเอง (เผื่อไว้สำหรับอนาคต ตอนนี้ระบบมีแต่ฝั่งร้าน) */
    CUSTOMER
}
