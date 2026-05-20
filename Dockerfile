FROM ubuntu:latest
LABEL authors="walter"

# Estágio de Build
FROM maven:3.9.16-eclipse-temurin-25 AS build
WORKDIR /app

# 1. Copia tudo do repositório para o container
COPY . .

# 2. Comando inteligente: Procura onde está o pom.xml e faz o build lá dentro
RUN find . -name "pom.xml" -execdir mvn clean package -DskipTests \;

# Estágio Final
FROM eclipse-temurin:25-jre
WORKDIR /app

# 3. Busca o arquivo .jar que foi gerado em qualquer subpasta 'target'
COPY --from=build /app/**/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]