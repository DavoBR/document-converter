# Use an Maven image as the base image
FROM maven:3-eclipse-temurin-21-alpine AS build
# Set the working directory in the container
WORKDIR /app
# Install LibreOffice
RUN apk add --update --no-cache libreoffice
# Copy the pom.xml and the project files to the container
COPY pom.xml .
COPY src ./src
# Build the application using Maven
RUN mvn package -DskipTests
RUN mv target/*.jar app.jar


# Use an JRE image as the base image
FROM eclipse-temurin:21-jre-alpine
# Install LibreOffice
RUN apk add --update --no-cache libreoffice
# Install the Microsoft fonts
RUN apk add --no-cache msttcorefonts-installer fontconfig && \
    update-ms-fonts && fc-cache -f
# Set the working directory 
WORKDIR /app/
# Copy the built JAR file from the previous stage to the container
COPY --from=build /app/app.jar .
# Make port available to the world outside this container
EXPOSE 8080
# Set the command to run the application
CMD [ "java", "-Dloader.path=/usr/lib/libreoffice/program/classes", "-jar", "app.jar"]