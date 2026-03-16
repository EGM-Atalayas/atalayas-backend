# Etapa 1: Build
FROM maven:3.9.6-eclipse-temurin-21 AS build

# Directorio de trabajo
WORKDIR /app

# Copiamos pom.xml y dependencias primero para cachear
COPY atalayas-backend/pom.xml .

# Instala dependencias (cachea para builds futuros)
RUN mvn dependency:go-offline

# Copiamos el código fuente
COPY atalayas-backend/src ./src

# Compilamos el JAR sin tests (más rápido)
RUN mvn clean package -DskipTests

# Etapa 2: Imagen de producción
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copiamos el JAR construido
COPY --from=build /app/target/*.jar app.jar

# Comando para ejecutar la app
CMD ["java", "-jar", "app.jar"]