# ---------- Estágio 1: compilar ----------
FROM eclipse-temurin:24-jdk AS build
WORKDIR /app

# Copia só o que define dependências primeiro: o Docker reaproveita essa camada em cache.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -DskipTests package

# ---------- Estágio 2: rodar ----------
FROM eclipse-temurin:24-jre
WORKDIR /app

# IMPORTANTE: usuário sem privilégios. Se a aplicação for explorada, o estrago é limitado.
RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /app/target/*.jar app.jar
USER app

EXPOSE 8080

# Marca o container como "healthy" só quando o Actuator responde.
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD ["sh", "-c", "wget -qO- http://localhost:8080/actuator/health | grep -q UP || exit 1"]

ENTRYPOINT ["java", "-jar", "app.jar"]