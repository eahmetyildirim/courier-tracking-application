# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Toolchains config — container already has JDK 21, point to JAVA_HOME
RUN mkdir -p /root/.m2 && \
    echo '<?xml version="1.0" encoding="UTF-8"?><toolchains><toolchain><type>jdk</type><provides><version>21</version></provides><configuration><jdkHome>'"${JAVA_HOME}"'</jdkHome></configuration></toolchain></toolchains>' > /root/.m2/toolchains.xml

COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/courier-tracking-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
