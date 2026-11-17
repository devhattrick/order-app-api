# Lita Order API

Backend สำหรับระบบสั่งอาหารร้านขนมปังซาวโดว์ **Lita**
Spring Boot 3 · Java 21 · PostgreSQL · JWT

---

## 1. รันบนเครื่อง

```bash
# 1) ยก Postgres ขึ้นมา (ฟรี ไม่กินโควต้า cloud)
docker compose up -d

# 2) รัน API
mvn spring-boot:run
```

API อยู่ที่ `http://localhost:8080`
Flyway จะสร้างตารางและใส่เมนูตั้งต้นให้เองตอนสตาร์ทครั้งแรก

### ผู้ใช้งานเริ่มต้น

| Username | Password | สิทธิ์ |
|---|---|---|
| `admin` | `admin1234` | ADMIN |
| `staff` | `staff1234` | STAFF |

สร้างโดย `UserSeeder` ตอนแอปสตาร์ท เปลี่ยนรหัสผ่านได้ด้วย env
`LITA_SEED_ADMIN_PASSWORD` / `LITA_SEED_STAFF_PASSWORD`

> **ก่อนขึ้น production ต้องเปลี่ยนรหัสผ่านทั้งสองบัญชี และตั้ง `JWT_SECRET` ใหม่**

---

## 2. ทดสอบ API

เปิดไฟล์ `api.http` ด้วย IntelliJ HTTP Client หรือ VS Code REST Client
แล้วกดรันทีละ request จากบนลงล่าง — token จะถูกเก็บให้อัตโนมัติ

ข้อ **14–17** คือการทดสอบสิทธิ์ตาม requirement โดยตรง

---

## 3. สิทธิ์การใช้งาน

กติกาทั้งหมดอยู่ที่ `config/SecurityConfig.java` ไฟล์เดียว

| Endpoint | ADMIN | STAFF | ไม่ login |
|---|---|---|---|
| `POST /api/auth/login` | ✅ | ✅ | ✅ |
| `GET /api/public/orders/{token}` | ✅ | ✅ | ✅ |
| `GET /api/dashboard/**` | ✅ | ❌ 403 | ❌ 401 |
| `POST/PUT/DELETE /api/menus/**` | ✅ | ❌ 403 | ❌ 401 |
| `POST/PUT/DELETE /api/option-groups/**` | ✅ | ❌ 403 | ❌ 401 |
| `PATCH /api/menus/{id}/availability` | ✅ | ✅ | ❌ 401 |
| ที่เหลือทั้งหมด (ออเดอร์ · ผังโต๊ะ · เช็คบิล) | ✅ | ✅ | ❌ 401 |

**ทำไม STAFF กดปิดขายเมนูได้ ทั้งที่แก้ไขเมนูไม่ได้**
"ของหมด" เป็นงานหน้างานที่เกิดกลางวันขายของ ถ้าต้องรอ ADMIN มากดให้
ลูกค้าจะสั่งของที่ไม่มีไปเรื่อย ๆ — ต่างจากการแก้ราคาหรือลบเมนูซึ่งเป็น
การเปลี่ยนข้อมูลหลัก จึงสงวนไว้ให้ ADMIN

---

## 4. Endpoint ทั้งหมด

### Auth
| Method | Path | หมายเหตุ |
|---|---|---|
| POST | `/api/auth/login` | คืน JWT + ข้อมูลผู้ใช้ |
| GET | `/api/auth/me` | ดูสิทธิ์ของ token ปัจจุบัน |

### เมนู
| Method | Path |
|---|---|
| GET | `/api/menus?category=&availableOnly=` |
| GET | `/api/menus/{id}` |
| POST | `/api/menus` |
| PUT | `/api/menus/{id}` |
| PATCH | `/api/menus/{id}/availability` |
| DELETE | `/api/menus/{id}` |

### ตัวเลือกเสริม
| Method | Path |
|---|---|
| GET | `/api/option-groups` |
| GET | `/api/option-groups/{id}/usage` |
| POST | `/api/option-groups` |
| PUT | `/api/option-groups/{id}` |
| DELETE | `/api/option-groups/{id}` |

### ออเดอร์
| Method | Path | หมายเหตุ |
|---|---|---|
| POST | `/api/orders` | สร้างออเดอร์ |
| GET | `/api/orders/active` | กระดานครัว |
| GET | `/api/orders/{id}` | |
| PATCH | `/api/orders/{id}/status` | `{ "status": "PREPARING" }` |
| GET | `/api/orders?from=&to=&status=&orderType=&keyword=&page=&size=` | ประวัติ |
| GET | `/api/orders/stream?token=` | SSE realtime |

### ผังโต๊ะ
| Method | Path |
|---|---|
| GET | `/api/tables` |
| POST | `/api/tables/{tableNo}/close` |

### สาธารณะ และ รายงาน
| Method | Path | สิทธิ์ |
|---|---|---|
| GET | `/api/public/orders/{trackingToken}` | ไม่ต้อง login |
| GET | `/api/dashboard/summary?days=14` | ADMIN |

---

## 5. Business Rules ที่บังคับไว้ฝั่ง server

| Rule | ทำที่ไหน |
|---|---|
| **ราคาคำนวณฝั่ง server เสมอ** — request ไม่มีช่องให้ส่งราคามา | `OrderService.buildItem` |
| ทานที่ร้านต้องมีโต๊ะ · ซื้อกลับต้องไม่มีโต๊ะ | `OrderService` + check constraint ใน DB |
| เลขที่เอกสาร/เลขคิวออกแบบ atomic รีเซ็ตรายวัน | `DocumentNumberService` |
| สถานะเดินทีละขั้นตาม flow ของแต่ละประเภทออเดอร์ | `OrderStatus.next()` |
| DINE_IN ปิดเป็น COMPLETED ได้จากการเช็คบิลเท่านั้น | `ShopTableService.closeTable` |
| ซื้อกลับ = prepaid · ทานที่ร้าน = postpaid | `OrderService.create` |
| กลุ่มตัวเลือกที่ required ต้องถูกเลือก · SINGLE เลือกซ้ำไม่ได้ | `OrderService.resolveOptions` |
| ชื่อ/ราคาตัวเลือกถูก snapshot ลงออเดอร์ | `OrderItemOption` |

**จุดที่ย้ายจาก frontend มาอยู่ที่นี่และสำคัญที่สุด** คือการออกเลขคิว
เดิม demo คำนวณจาก localStorage ซึ่งถ้าพนักงานสองเครื่องกดพร้อมกัน
จะได้เลขคิวซ้ำกัน ตอนนี้ใช้ `INSERT ... ON CONFLICT DO UPDATE ... RETURNING`
ของ Postgres ซึ่ง atomic ในคำสั่งเดียว ปลอดภัยแม้รัน API หลาย instance

---

## 6. ตัวแปร Environment สำหรับ production

| ตัวแปร | ค่าเริ่มต้น | ต้องตั้งใหม่ตอน deploy |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/lita_order` | ✅ ชี้ไป Neon |
| `DB_USERNAME` / `DB_PASSWORD` | `lita` / `lita_dev_password` | ✅ |
| `JWT_SECRET` | ค่า dev | ✅ **ต้องเปลี่ยน** อย่างน้อย 32 ตัวอักษร |
| `JWT_EXPIRATION_HOURS` | `12` | ตามนโยบาย |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | ✅ ใส่ URL ของ frontend จริง |
| `PORT` | `8080` | Cloud Run ตั้งให้เอง |

---

## 7. โครงสร้างโปรเจกต์

จัดแบบ **package-by-feature** ไม่ใช่ package-by-layer
เปิดโฟลเดอร์เดียวแล้วเห็นครบทั้ง entity, repository, service, controller ของเรื่องนั้น

```
com.lita.orderapi
├── auth/        login, JWT, filter
├── user/        ผู้ใช้งานและสิทธิ์
├── menu/        เมนู
├── option/      กลุ่มตัวเลือกเสริม
├── order/       ออเดอร์ · เลขที่เอกสาร · SSE
├── shoptable/   ผังโต๊ะ · เช็คบิล
├── dashboard/   รายงานยอดขาย
├── common/      error handling ส่วนกลาง
└── config/      SecurityConfig
```

---

## 8. ข้อจำกัดที่รู้อยู่ และควรแก้ก่อนใช้งานหนัก

| เรื่อง | ตอนนี้ | ควรเป็น |
|---|---|---|
| Dashboard | กวาดออเดอร์ในช่วงมาคำนวณใน memory | ตารางสรุปรายวัน (pre-aggregate) |
| ความสัมพันธ์ของ order | โหลดแบบ EAGER ทั้งหมด | ปรับเป็น LAZY + fetch join เมื่อข้อมูลโตขึ้น |
| SSE | เก็บ emitter ไว้ใน memory ของ instance เดียว | ถ้ารันหลาย instance ต้องมี Redis pub/sub คั่น |
| Refresh token | ยังไม่มี — token หมดอายุแล้ว login ใหม่ | เพิ่ม refresh token ถ้าอายุ token สั้นลง |
| จัดการผู้ใช้ | seed ตอนสตาร์ทอย่างเดียว | หน้าจัดการ user สำหรับ ADMIN |
