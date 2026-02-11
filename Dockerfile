FROM eclipse-temurin:17-jdk-jammy as builder

WORKDIR /build

COPY pom.xml .
COPY src ./src

RUN apt-get update && apt-get install -y maven && \
    mvn clean package -DskipTests && \
    apt-get remove -y maven && \
    apt-get autoremove -y

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]