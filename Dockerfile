

# Use Amazon Corretto 21 JDK
FROM amazoncorretto:21

# author
LABEL authors="abdulhamid-mazroua"

# Set working directory
WORKDIR /app

# Copy your IntelliJ-built JAR
COPY out/artifacts/hameed_password_manager_jar/hameed-password-manager.jar /app/hameed-password-manager.jar

# Expose JDWP debug port
EXPOSE 5005

# Run the JAR with remote debugging enabled
CMD ["java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", "-jar", "hameed-password-manager.jar"]
