# ---- Build stage ----
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Mejor cache: primero dependencias
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

# Ahora el código
COPY src ./src

# Empaqueta
RUN mvn -q -DskipTests package

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copia el jar generado por Maven
COPY --from=build /app/target/*.jar app.jar

# Puerto típico Spring (ajustable si usas otro)
EXPOSE 8080

# Permite pasar flags: -e JAVA_OPTS="..."
ENV JAVA_OPTS=""

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]