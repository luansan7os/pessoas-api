# Build em duas etapas: a imagem final nao carrega Maven nem codigo-fonte.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# As dependencias sao baixadas numa camada propria: mudar codigo nao refaz o download.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Nao roda como root.
RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /app/target/pessoas-api-1.0.0.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
