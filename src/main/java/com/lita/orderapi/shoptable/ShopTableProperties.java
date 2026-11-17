package com.lita.orderapi.shoptable;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * ผังโต๊ะของร้าน อ่านจาก application.yml
 *
 * ทำเป็น config เพราะจำนวนโต๊ะเป็นเรื่องของแต่ละสาขา
 * เพิ่มหรือลดโต๊ะไม่ควรต้องแก้โค้ดแล้ว deploy ใหม่
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "lita")
public class ShopTableProperties {

    private List<ShopTable> tables = new ArrayList<>();

    @Getter
    @Setter
    public static class ShopTable {
        /**
         * ตั้งชื่อ field ว่า tableNo ไม่ใช่ no เพราะ YAML 1.1 จะแปลง
         * key ที่ชื่อ no ให้กลายเป็น boolean false
         */
        private String tableNo;
        private int seats;
    }
}
