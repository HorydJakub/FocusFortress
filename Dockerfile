# --- ETAP 1: Budowanie (Maven + Java 17) ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# 1. Kopiujemy TYLKO plik konfiguracyjny
COPY pom.xml .

# 2. TO JEST NOWOŚĆ: Pobieramy zależności ZANIM skopiujemy kod
# Dzięki temu ta warstwa zostanie w cache, dopóki nie zmienisz pom.xml
RUN mvn dependency:go-offline -B

# 3. Dopiero teraz kopiujemy kod źródłowy
COPY backend ./backend

# 4. Budujemy aplikację (korzystając z pobranych wcześniej bibliotek)
# Flaga -o oznacza "offline" - nie próbuj łączyć się z internetem po biblioteki
RUN mvn clean package -DskipTests -o

# --- ETAP 2: Uruchamianie (Sama Java 17) ---
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]