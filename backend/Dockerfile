# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# settings.xml: força o Maven a usar SOMENTE o Maven Central.
# (o flyway-parent declara repositórios extras — repo.gradle.org, redshift,
#  flyway-community-db-support/GitHub Packages, red-gate — que deixam a
#  resolução lenta e baixam BOMs irrelevantes como google-cloud-bigquery*)
COPY settings.xml /app/settings.xml

# Camada de dependências: só é reconstruída quando o pom.xml muda.
# Sem "dependency:go-offline" (essa meta baixa o universo inteiro, inclusive
# BOMs irrelevantes como google-cloud-bigquery*); "dependency:resolve"
# baixa só o necessário, direto do Maven Central.
COPY pom.xml .
RUN mvn -B -s /app/settings.xml dependency:resolve dependency:resolve-plugins

# Camada de build: quando só o src muda, o Docker reutiliza a camada acima
# (dependências já em /root/.m2) e apenas recompila.
COPY src ./src
RUN mvn -B -s /app/settings.xml clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]