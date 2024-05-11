FROM mcr.microsoft.com/devcontainers/java:1-21-bullseye

# Install LibreOffice and Maven
RUN apt update && apt install -y libreoffice maven
# Install the Microsoft fonts
RUN apt update && \
    apt install -y software-properties-common && \
    add-apt-repository -y contrib && add-apt-repository -y non-free && \
    apt update && \
    apt install -y ttf-mscorefonts-installer