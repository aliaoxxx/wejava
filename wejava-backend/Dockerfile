# syntax=docker/dockerfile:1

# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# 先只复制 pom 预热依赖，源码改动时可以复用这一层缓存
COPY wejava-backend/pom.xml ./wejava-backend/pom.xml
RUN mvn -B -ntp -f wejava-backend/pom.xml -Pdocker dependency:go-offline -DskipTests || true

COPY wejava-backend/src ./wejava-backend/src
RUN mvn -B -ntp -f wejava-backend/pom.xml clean package -Pdocker -DskipTests

# ---------- 运行阶段 ----------
FROM eclipse-temurin:17-jre

LABEL maintainer="WeJavaAdmin"

WORKDIR /app

# 演示环境用固定堆：机器内存小的时候行为可预期；生产可自行调大或用 MaxRAMPercentage
ENV TZ=Asia/Shanghai \
    LANG=C.UTF-8 \
    SPRING_PROFILES_ACTIVE=docker \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:MaxMetaspaceSize=192m -Djava.security.egd=file:/dev/./urandom -Duser.timezone=Asia/Shanghai"

# 容器运行时自动挂载 /tmp 为匿名卷
VOLUME /tmp

# 可执行 WAR：provided 的 Tomcat 在 lib-provided 中，java -jar 可直接启动
COPY --from=build /workspace/wejava-backend/target/wejava.war /app/app.war

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.war"]
