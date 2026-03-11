# 1. Etapa de Construcción (Usamos una imagen con Maven y Java 21)
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
# Copiamos el pom y el código fuente
COPY pom.xml .
COPY src ./src
# Compilamos saltando los tests para ir más rápido
RUN mvn clean package -DskipTests

# 2. Etapa de Ejecución (Una imagen más ligera solo con Java)
FROM eclipse-temurin:21-jre
WORKDIR /app
# Copiamos el archivo .jar que se generó en el paso anterior
COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar
# Exponemos el puerto
EXPOSE 8080
# Comando para encender el motor
ENTRYPOINT ["java", "-jar", "app.jar"]