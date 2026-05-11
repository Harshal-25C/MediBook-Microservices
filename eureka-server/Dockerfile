FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring
COPY target/*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java","-Xms64m","-Xmx192m","-Dspring.profiles.active=docker","-jar","app.jar"]