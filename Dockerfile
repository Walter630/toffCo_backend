# Estágio de Build
FROM maven:3.9.16-eclipse-temurin-25 AS build
WORKDIR /app

# 1. Copia apenas o pom.xml primeiro
COPY pom.xml .

# 2. Baixa as dependências e joga no cache do Docker
RUN mvn dependency:go-offline -B

# 3. Agora sim, copia o código-fonte (que muda o tempo todo)
COPY src ./src

# 4. Faz o build sem precisar baixar nada de fora
RUN mvn clean package -DskipTests

# Estágio Final (Permanece igual, leve e seguro)
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "--enable-preview", "-jar", "app.jar"]