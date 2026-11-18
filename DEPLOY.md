# Deploy — Lita Order App

คู่มือเอาระบบขึ้นใช้งานจริง ทำตามลำดับจากบนลงล่าง

| ส่วน | ที่อยู่ | ค่าใช้จ่าย |
|---|---|---|
| Database | Neon (PostgreSQL) — Singapore | ฿0 (free tier) |
| Backend | Google Cloud Run — `asia-southeast1` | ฿0 (free tier) |
| Frontend | GitHub Pages | ฿0 |

> **ต้องมีบัตรเครดิตผูกกับ Google Cloud** แม้จะใช้แค่ free tier
> ขั้นตอนที่ 2.4 ตั้ง budget alert ไว้กันค่าใช้จ่ายหลุด อย่าข้าม

---

## 1. Database — Neon

### 1.1 สร้าง project

1. สมัครที่ https://neon.tech (ล็อกอินด้วย GitHub ได้)
2. Create project
   - **Region: `AWS ap-southeast-1 (Singapore)`** — ใกล้ไทยที่สุด และอยู่ภูมิภาคเดียวกับ Cloud Run ที่จะสร้างในขั้นถัดไป
   - Postgres version: 16
   - Database name: `lita_order`

### 1.2 เอา connection string

หน้า Dashboard → **Connection string**

> ⚠️ **เลือกแบบ Direct connection ไม่ใช่ Pooled connection**
>
> Flyway ใช้ advisory lock ของ Postgres ตอนรัน migration ซึ่งทำงานกับ connection pooler
> (PgBouncer แบบ transaction mode ที่ Neon ใช้) ไม่ได้ — migration จะค้างหรือพังแบบงง ๆ
> ถ้า URL มีคำว่า `-pooler` อยู่ แสดงว่าหยิบผิดอัน

จะได้หน้าตาแบบนี้

```
postgresql://lita_owner:AbCd1234@ep-cool-name-123456.ap-southeast-1.aws.neon.tech/lita_order?sslmode=require
```

แยกออกเป็น 3 ส่วนเก็บไว้ใช้ขั้นถัดไป

| ตัวแปร | ค่า |
|---|---|
| `DB_URL` | `jdbc:postgresql://ep-cool-name-123456.ap-southeast-1.aws.neon.tech/lita_order?sslmode=require` |
| `DB_USERNAME` | `lita_owner` |
| `DB_PASSWORD` | `AbCd1234` |

สังเกตว่า `DB_URL` ต้องขึ้นต้นด้วย **`jdbc:`** และ **ตัด `user:pass@` ออก** เพราะ Spring รับแยกกัน

---

## 2. Backend — Cloud Run

### 2.1 เตรียมเครื่อง

```bash
# ติดตั้ง gcloud CLI (ถ้ายังไม่มี)
brew install --cask google-cloud-sdk

gcloud auth login
```

### 2.2 สร้าง project และเปิด API

```bash
export PROJECT_ID=lita-order-app          # ต้องไม่ซ้ำกับใครทั้งโลก เติมตัวเลขต่อท้ายได้
export REGION=asia-southeast1             # Singapore

gcloud projects create $PROJECT_ID
gcloud config set project $PROJECT_ID
```

จากนั้นไปผูกบัญชีเรียกเก็บเงินที่ https://console.cloud.google.com/billing แล้วกลับมารัน

```bash
gcloud services enable run.googleapis.com \
                       cloudbuild.googleapis.com \
                       secretmanager.googleapis.com \
                       artifactregistry.googleapis.com
```

### 2.3 เก็บความลับใน Secret Manager

ไม่ใส่รหัสผ่านตรง ๆ ในคำสั่ง deploy เพราะมันจะไปโผล่ใน shell history และหน้า config ของ Cloud Run ที่ทุกคนในโปรเจกต์เห็น

```bash
# JWT secret — สุ่มใหม่ ห้ามใช้ค่า default ใน application.yml เด็ดขาด
# ใครรู้ค่านี้ ปลอม token เป็น ADMIN ได้ทันที
openssl rand -base64 48 | tr -d '\n' | gcloud secrets create jwt-secret --data-file=-

# Database
printf '%s' 'jdbc:postgresql://ep-xxxx.ap-southeast-1.aws.neon.tech/lita_order?sslmode=require' \
  | gcloud secrets create db-url --data-file=-
printf '%s' 'lita_owner'  | gcloud secrets create db-username --data-file=-
printf '%s' 'AbCd1234'    | gcloud secrets create db-password --data-file=-

# รหัสผ่านพนักงาน — ตั้งเองให้แข็งแรง ใช้ครั้งเดียวตอนสตาร์ทครั้งแรก
printf '%s' 'ตั้งรหัสแอดมินตรงนี้' | gcloud secrets create seed-admin-password --data-file=-
printf '%s' 'ตั้งรหัสพนักงานตรงนี้' | gcloud secrets create seed-staff-password --data-file=-
```

ให้ service account ของ Cloud Run อ่าน secret ได้

```bash
PROJECT_NUMBER=$(gcloud projects describe $PROJECT_ID --format='value(projectNumber)')
SA="${PROJECT_NUMBER}-compute@developer.gserviceaccount.com"

for s in jwt-secret db-url db-username db-password seed-admin-password seed-staff-password; do
  gcloud secrets add-iam-policy-binding $s \
    --member="serviceAccount:${SA}" \
    --role="roles/secretmanager.secretAccessor"
done
```

### 2.4 ตั้ง budget alert ⚠️ อย่าข้าม

free tier ไม่ได้แปลว่าจ่ายไม่ได้ ถ้ามีอะไรผิดปกติ (โดนยิง, โค้ดวนลูป) บิลวิ่งได้

ไปที่ https://console.cloud.google.com/billing → **Budgets & alerts** → Create budget
- จำนวน: **$5**
- แจ้งเตือนที่ 50% / 90% / 100%

### 2.5 Deploy

```bash
cd ~/Project/MyProject/order-app-api

gcloud run deploy order-app-api \
  --source . \
  --region $REGION \
  --allow-unauthenticated \
  --memory 512Mi \
  --cpu 1 \
  --min-instances 0 \
  --max-instances 2 \
  --set-secrets "JWT_SECRET=jwt-secret:latest,\
DB_URL=db-url:latest,\
DB_USERNAME=db-username:latest,\
DB_PASSWORD=db-password:latest,\
LITA_SEED_ADMIN_PASSWORD=seed-admin-password:latest,\
LITA_SEED_STAFF_PASSWORD=seed-staff-password:latest" \
  --set-env-vars "CORS_ALLOWED_ORIGINS=https://devhattrick.github.io"
```

ครั้งแรกใช้เวลา 5–10 นาที เพราะต้อง build image และโหลด dependency ของ Maven ทั้งหมด

**`--max-instances 2` สำคัญ** — กันกรณีมีอะไรผิดปกติแล้ว Cloud Run ขยายเป็น 100 instance จนบิลพุ่ง สาขาเดียวไม่มีทางต้องใช้เกิน 2

เสร็จแล้วจะได้ URL มา เก็บไว้

```
https://order-app-api-xxxxxxxxx.asia-southeast1.run.app
```

### 2.6 ทดสอบว่า backend ขึ้นจริง

```bash
export API=https://order-app-api-xxxxxxxxx.asia-southeast1.run.app

# ต้องได้ 401 พร้อม JSON ภาษาไทย — แปลว่า security ทำงาน
curl -i $API/api/orders/active

# ต้องได้ token กลับมา
curl -i -X POST $API/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"รหัสที่ตั้งไว้ใน secret"}'
```

ถ้าไม่ผ่าน ดู log ด้วย

```bash
gcloud run services logs read order-app-api --region $REGION --limit 50
```

---

## 3. Frontend — GitHub Pages

### 3.1 ตั้ง repository variable

GitHub → repo `order-app` → **Settings → Secrets and variables → Actions → Variables** → New repository variable

| Name | Value |
|---|---|
| `VITE_API_BASE_URL` | URL ของ Cloud Run จากข้อ 2.5 (ไม่ต้องมี `/` ปิดท้าย) |

ใช้ **Variables ไม่ใช่ Secrets** เพราะค่านี้ถูกฝังลงไฟล์ JS ที่ใครเปิดดูก็เห็น เก็บเป็น secret จะหลอกตัวเองว่ามันถูกปกปิด

### 3.2 เปิด GitHub Pages

Settings → Pages → Source: **GitHub Actions**

### 3.3 Deploy

```bash
cd ~/Project/MyProject/Order\ App
git checkout -B production
git merge main          # ถ้า production มีอยู่แล้ว
git push origin production
```

ดูผลที่แท็บ Actions ถ้าลืมตั้ง variable ขั้น 3.1 workflow จะหยุดพร้อมบอกเหตุผล ไม่ปล่อยให้ deploy ของพัง

ได้หน้าเว็บที่ `https://devhattrick.github.io/order-app/`

---

## 4. เช็คให้ครบก่อนส่งมอบ

| # | ทดสอบ | ผ่านเมื่อ |
|---|---|---|
| 1 | เปิดหน้าเว็บ | เด้งไป `/login` |
| 2 | login `admin` | เข้าได้ เห็นเมนู Dashboard |
| 3 | login `staff` | เข้าได้ **ไม่เห็น** Dashboard และไม่มีปุ่มแก้เมนู |
| 4 | เปิด DevTools → Network | `active` ยิงทุก 5 วินาที ไปที่ domain ของ Cloud Run |
| 5 | คีย์ออเดอร์ | เลขคิวและยอดเงินถูกต้อง |
| 6 | เปิด 2 เครื่องพร้อมกัน | เปลี่ยนสถานะแล้วอีกเครื่องตามภายใน 5 วินาที |
| 7 | สแกน QR บนใบเสร็จด้วยมือถือที่ **ปิด Wi-Fi ใช้ 4G** | เห็นสถานะออเดอร์ |

ข้อ 7 สำคัญที่สุด เพราะพิสูจน์ว่าระบบออกสู่อินเทอร์เน็ตจริง ไม่ได้ทำงานได้เพราะบังเอิญอยู่วง Wi-Fi เดียวกัน

---

## 5. เรื่องที่ต้องรู้หลัง deploy

### Cold start ตอนเช้า

Cloud Run ปิด instance เมื่อไม่มีใครใช้ ลูกค้าคนแรกของวันจะรอ **10–20 วินาที** เพราะ JVM ต้องสตาร์ทใหม่
ระหว่างวันไม่เจอ เพราะหน้าร้าน poll ทุก 5 วินาที instance จึงไม่หลับ

แก้ได้ 2 ทาง
- **ฟรี**: เปิดหน้าเว็บทิ้งไว้ก่อนร้านเปิด 1 นาที
- **เสียเงิน**: `--min-instances 1` แต่จะโดนคิดเงินตลอด 24 ชม. ประมาณ **฿1,900/เดือน** ซึ่งไม่คุ้มสำหรับการประหยัดเวลา 15 วินาทีวันละครั้ง

### รหัสผ่าน seed ใช้แค่ครั้งแรก

`UserSeeder` สร้าง user เฉพาะตอนที่ยังไม่มีในฐานข้อมูล เปลี่ยนค่าใน Secret Manager ทีหลังจะไม่มีผล
ต้องไปแก้ `password_hash` ใน database โดยตรง (วิธีทำอยู่ใน `README.md`)

### Deploy เวอร์ชันใหม่

```bash
# Backend — รันคำสั่งเดิมซ้ำ Cloud Run จะค่อย ๆ สลับ traffic ให้เอง ไม่มีช่วงล่ม
cd ~/Project/MyProject/order-app-api && gcloud run deploy order-app-api --source . --region $REGION

# Frontend
cd ~/Project/MyProject/Order\ App && git checkout production && git merge main && git push
```

### ย้อนกลับเวอร์ชันเก่า

```bash
gcloud run revisions list --service order-app-api --region $REGION
gcloud run services update-traffic order-app-api --region $REGION --to-revisions REVISION_NAME=100
```

### Backup database

Neon free tier เก็บประวัติย้อนหลังให้ 7 วัน (point-in-time restore) แต่ถ้าโปรเจกต์โดนลบคือหายหมด
เดือนละครั้งควร dump เก็บไว้เอง

```bash
pg_dump "postgresql://lita_owner:AbCd1234@ep-xxxx.ap-southeast-1.aws.neon.tech/lita_order?sslmode=require" \
  -Fc -f "lita-$(date +%Y%m%d).dump"
```
