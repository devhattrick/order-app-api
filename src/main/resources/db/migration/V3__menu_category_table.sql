-- ---------------------------------------------------------------
-- ย้ายหมวดหมู่เมนูจาก enum ในโค้ด มาเป็นตารางใน database
-- เพื่อให้ร้านเพิ่ม / แก้ชื่อ / ลบหมวดหมู่เองได้ โดยไม่ต้อง deploy ใหม่
--
-- เขียนแบบ idempotent (รันซ้ำได้ไม่พัง) เพราะอาจถูกรันมือใน DBeaver
-- ก่อนที่ Flyway จะรันอัตโนมัติตอน deploy
-- ---------------------------------------------------------------

-- 1. ตารางหมวดหมู่
create table if not exists menu_category (
    id         uuid        primary key,
    name       varchar(60) not null unique,
    name_en    varchar(60),
    sort_order integer     not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

-- 2. ย้ายหมวดหมู่เดิม 4 หมวดเข้าตาราง
--    ใช้ UUID ตายตัวเพื่อให้ dev กับ production มี id ตรงกัน
insert into menu_category (id, name, name_en, sort_order) values
    ('d0000000-0000-4000-8000-000000000001', 'แซนด์วิช & โทสต์', 'Sandwiches', 1),
    ('d0000000-0000-4000-8000-000000000002', 'ขนมปังซาวโดว์',   'Loaves',     2),
    ('d0000000-0000-4000-8000-000000000003', 'เครื่องดื่ม',       'Drinks',     3),
    ('d0000000-0000-4000-8000-000000000004', 'ขนม',             'Sweets',     4)
on conflict (id) do nothing;

-- 3. ผูกเมนูเข้ากับหมวดหมู่
alter table menu_item add column if not exists category_id uuid;

-- 4. ย้ายข้อมูลจากคอลัมน์เดิม (ทำเฉพาะตอนที่คอลัมน์เก่ายังอยู่)
do $$
begin
    if exists (
        select 1 from information_schema.columns
        where table_name = 'menu_item' and column_name = 'category'
    ) then
        update menu_item m
        set category_id = v.id
        from (values
            ('SANDWICH', 'd0000000-0000-4000-8000-000000000001'::uuid),
            ('BREAD',    'd0000000-0000-4000-8000-000000000002'::uuid),
            ('DRINK',    'd0000000-0000-4000-8000-000000000003'::uuid),
            ('DESSERT',  'd0000000-0000-4000-8000-000000000004'::uuid)
        ) as v(code, id)
        where m.category = v.code and m.category_id is null;
    end if;
end $$;

-- เมนูที่หาหมวดเดิมไม่เจอ (ข้อมูลเพี้ยน) ให้ไปอยู่หมวดแรกไว้ก่อน
-- ดีกว่าปล่อยให้ migration ล้มแล้วแอปสตาร์ทไม่ขึ้นทั้งระบบ
update menu_item
set category_id = 'd0000000-0000-4000-8000-000000000001'
where category_id is null;

-- 5. บังคับให้ทุกเมนูต้องมีหมวด และผูก foreign key
alter table menu_item alter column category_id set not null;

do $$
begin
    if not exists (
        select 1 from information_schema.table_constraints
        where constraint_name = 'fk_menu_item_category'
    ) then
        alter table menu_item
            add constraint fk_menu_item_category
            foreign key (category_id) references menu_category (id);
    end if;
end $$;

create index if not exists idx_menu_item_category on menu_item (category_id);

-- 6. ทิ้งคอลัมน์เดิมและ check constraint ที่ตรึงชื่อหมวดไว้ 4 ค่า
alter table menu_item drop constraint if exists chk_menu_category;
alter table menu_item drop column if exists category;

-- ---------------------------------------------------------------
-- 7. order_item.category เป็น snapshot ชื่อหมวด ณ เวลาที่สั่ง
--
--    ตั้งใจไม่ผูก foreign key ไปที่ menu_category เพราะถ้าร้านเปลี่ยนชื่อ
--    หรือลบหมวดทีหลัง ใบเสร็จและรายงานย้อนหลังต้องไม่เปลี่ยนตาม
--    หลักการเดียวกับที่ order_item_option เก็บชื่อตัวเลือกไว้เอง
-- ---------------------------------------------------------------
alter table order_item alter column category type varchar(60);

update order_item set category =
    case category
        when 'SANDWICH' then 'แซนด์วิช & โทสต์'
        when 'BREAD'    then 'ขนมปังซาวโดว์'
        when 'DRINK'    then 'เครื่องดื่ม'
        when 'DESSERT'  then 'ขนม'
        else category
    end
where category in ('SANDWICH', 'BREAD', 'DRINK', 'DESSERT');
