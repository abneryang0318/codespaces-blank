# =========================
# 第一階段：Build JAR
# =========================
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# 先只複製 pom.xml，利用快取把依賴抓好
COPY pom.xml .
RUN mvn -q dependency:go-offline

# 再複製原始碼，正式進行 build
COPY src ./src

# 打包 Spring Boot JAR（跳過測試）
RUN mvn -q -DskipTests clean package


# =========================
# 第二階段：Run JAR
# =========================
FROM eclipse-temurin:17-jre

WORKDIR /app

# 從 builder 階段把 fat JAR 拿過來
COPY --from=builder /app/target/*.jar app.jar

# Cloud Run 會注入 PORT 環境變數，我們預設 8080
ENV PORT=8080
ENV TZ=Asia/Taipei

EXPOSE 8080

# 用 -Dserver.port=${PORT} 讓 Spring Boot 跟 Cloud Run 的 PORT 對齊
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT} -jar app.jar"]
