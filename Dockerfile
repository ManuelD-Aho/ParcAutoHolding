FROM openjdk:21-slim

WORKDIR /app

# Copier les fichiers JAR et ressources
COPY target/*.jar app.jar
COPY lib/ lib/

# Exposer le port si nécessaire pour JavaFX
# EXPOSE 8080

# Définir les variables d'environnement
ENV MYSQL_HOST=db
ENV MYSQL_PORT=3306
ENV MYSQL_DATABASE=ParcAuto
ENV TZ=Europe/Paris

# Commande de démarrage
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-cp", "app.jar:lib/*", "main.java.com.miage.parcauto.MainApp"]