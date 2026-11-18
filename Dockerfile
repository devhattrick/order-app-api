# syntax=docker/dockerfile:1

# ---------------------------------------------------------------- #
# Stage 1 — build
# ---------------------------------------------------------------- #
# คัดลอก pom.xml เข้ามาก่อน src เพื่อให้ Docker cache ชั้น dependency ไว้
# แก้โค้ดแล้ว build รอบถัดไปจะไม่โหลด dependency ใหม่ทั้งหมด
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests


# ---------------------------------------------------------------- #
# Stage 2 — runtime
# ---------------------------------------------------------------- #
# ใช้ JRE ไม่ใช่ JDK และใช้ alpine เพื่อให้ image เล็ก
# image เล็ก = Cloud Run ดึงมารันเร็วขึ้น = cold start สั้นลง
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# ไม่รันด้วย root — ถ้าโดนเจาะ จะทำอะไรต่อได้ยากขึ้น
RUN addgroup -S lita && adduser -S lita -G lita
USER lita

COPY --from=build --chown=lita:lita /build/target/*.jar app.jar

# MaxRAMPercentage  ให้ JVM รู้ขนาด memory ที่ container ได้จริง
#                   ถ้าไม่ตั้ง JVM จะเดาจากเครื่องโฮสต์แล้วโดน kill เพราะใช้เกิน
# UseSerialGC       GC ตัวเล็กที่สุด เหมาะกับ 1 vCPU และ heap เล็ก
#                   ใช้ memory น้อยกว่าและสตาร์ทเร็วกว่า G1 ที่เป็นค่าเริ่มต้น
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+UseSerialGC -Djava.security.egd=file:/dev/./urandom"

# Cloud Run ส่ง port มาทาง environment variable ชื่อ PORT
# application.yml อ่านค่านี้อยู่แล้วผ่าน ${PORT:8080}
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
