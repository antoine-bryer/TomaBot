# -----------------------------
# Étape 1 : Build du JAR avec Maven
# -----------------------------
FROM maven:3.9.3-eclipse-temurin-17 AS build

# Définir le répertoire de travail
WORKDIR /tomabotapp

# Copier les fichiers Maven
COPY pom.xml .
COPY src ./src

# Build du projet et création du JAR
RUN mvn clean package -DskipTests

# -----------------------------
# Étape 2 : Image finale avec OpenJDK
# -----------------------------
FROM eclipse-temurin:17-jdk-jammy

# Répertoire de travail dans le conteneur
WORKDIR /tomabotapp

# Copier le JAR depuis l'étape build
COPY --from=build /tomabotapp/target/*.jar tomabotapp.jar

# Exposer le port utilisé par Spring Boot
EXPOSE 8080

# Commande pour lancer l'application
ENTRYPOINT ["java","-jar","tomabotapp.jar"]
