-- =============================================================
-- V1: โครงสร้างฐานข้อมูลเริ่มต้นของระบบสั่งอาหาร Lita
-- =============================================================

-- ผู้ใช้งานระบบหน้าร้าน (ไม่มีลูกค้า — ลูกค้าดูสถานะผ่าน QR แบบไม่ต้อง login)
create table app_user (
    id            uuid         primary key,
    username      varchar(50)  not null unique,
    password_hash varchar(100) not null,
    display_name  varchar(100) not null,
    role          varchar(20)  not null,
    active        boolean      not null default true,
    created_at    timestamptz  not null default now(),
    constraint chk_user_role check (role in ('ADMIN', 'STAFF'))
);

-- ---------------------------------------------------------------
-- เมนูและตัวเลือกเสริม
-- ---------------------------------------------------------------

-- กลุ่มตัวเลือกเสริม เช่น "ขนาด", "ระดับความหวาน" — ใช้ซ้ำได้หลายเมนู
create table option_group (
    id                uuid         primary key,
    name              varchar(100) not null,
    type              varchar(10)  not null,
    required          boolean      not null default false,
    default_choice_id uuid,
    sort_order        integer      not null default 0,
    constraint chk_option_group_type check (type in ('SINGLE', 'MULTI'))
);

-- ตัวเลือกย่อยในกลุ่ม เช่น "Regular", "Large +20"
create table option_choice (
    id              uuid          primary key,
    option_group_id uuid          not null references option_group (id) on delete cascade,
    name            varchar(100)  not null,
    price_delta     numeric(10,2) not null default 0,
    sort_order      integer       not null default 0
);
create index idx_option_choice_group on option_choice (option_group_id);

create table menu_item (
    id          uuid          primary key,
    name        varchar(120)  not null,
    name_th     varchar(120),
    description varchar(255),
    price       numeric(10,2) not null,
    category    varchar(20)   not null,
    emoji       varchar(16),
    image_url   text,
    available   boolean       not null default true,
    sort_order  integer       not null default 0,
    created_at  timestamptz   not null default now(),
    updated_at  timestamptz   not null default now(),
    constraint chk_menu_category check (category in ('SANDWICH', 'BREAD', 'DRINK', 'DESSERT')),
    constraint chk_menu_price check (price >= 0)
);

-- เมนูผูกกับกลุ่มตัวเลือกแบบ many-to-many
create table menu_option_group (
    menu_item_id    uuid not null references menu_item (id) on delete cascade,
    option_group_id uuid not null references option_group (id) on delete cascade,
    primary key (menu_item_id, option_group_id)
);

-- ---------------------------------------------------------------
-- ออเดอร์
-- ---------------------------------------------------------------

create table shop_order (
    id             uuid          primary key,
    order_no       varchar(20)   not null unique,
    queue_no       varchar(10)   not null,
    tracking_token varchar(16)   not null unique,
    order_type     varchar(10)   not null,
    table_no       varchar(10),
    status         varchar(12)   not null,
    source         varchar(10)   not null,
    note           varchar(255),
    total_amount   numeric(10,2) not null,
    paid_at        timestamptz,
    created_at     timestamptz   not null,
    updated_at     timestamptz   not null,
    constraint chk_order_type check (order_type in ('DINE_IN', 'TAKEAWAY')),
    constraint chk_order_source check (source in ('POS', 'CUSTOMER')),
    constraint chk_order_status check (status in ('PENDING', 'PREPARING', 'READY', 'SERVED', 'COMPLETED', 'CANCELLED')),
    -- กติกาธุรกิจ: ทานที่ร้านต้องมีโต๊ะเสมอ / ซื้อกลับต้องไม่มีโต๊ะ
    constraint chk_order_table_matches_type check (
        (order_type = 'DINE_IN' and table_no is not null)
        or (order_type = 'TAKEAWAY' and table_no is null)
    )
);
create index idx_order_created_at on shop_order (created_at);
create index idx_order_status on shop_order (status);
create index idx_order_table_no on shop_order (table_no);

-- เก็บชื่อและราคา ณ เวลาที่สั่ง (snapshot) เพื่อให้ใบเสร็จเก่าไม่เปลี่ยนตามเมนูปัจจุบัน
create table order_item (
    id         uuid          primary key,
    order_id   uuid          not null references shop_order (id) on delete cascade,
    menu_id    uuid,
    menu_name  varchar(120)  not null,
    category   varchar(20)   not null,
    emoji      varchar(16),
    qty        integer       not null,
    unit_price numeric(10,2) not null,
    line_total numeric(10,2) not null,
    note       varchar(255),
    sort_order integer       not null default 0,
    constraint chk_order_item_qty check (qty > 0)
);
create index idx_order_item_order on order_item (order_id);

create table order_item_option (
    id            uuid          primary key,
    order_item_id uuid          not null references order_item (id) on delete cascade,
    group_id      uuid,
    group_name    varchar(100)  not null,
    choice_id     uuid,
    choice_name   varchar(100)  not null,
    price_delta   numeric(10,2) not null default 0
);
create index idx_order_item_option_item on order_item_option (order_item_id);

create table order_status_log (
    id         uuid        primary key,
    order_id   uuid        not null references shop_order (id) on delete cascade,
    status     varchar(12) not null,
    changed_by varchar(50),
    at         timestamptz not null
);
create index idx_order_status_log_order on order_status_log (order_id);

-- ---------------------------------------------------------------
-- ตัวนับเลขที่เอกสารรายวัน — รีเซ็ตเองทุกวันเพราะ key มีวันที่อยู่ด้วย
-- ---------------------------------------------------------------
create table daily_counter (
    business_date date        not null,
    scope         varchar(20) not null,
    value         integer     not null,
    primary key (business_date, scope)
);
