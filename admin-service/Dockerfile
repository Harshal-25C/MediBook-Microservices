FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring
COPY target/*.jar app.jar
EXPOSE 8089 
ENTRYPOINT ["java","-Xms48m","-Xmx192m","-Dspring.profiles.active=docker","-jar","app.jar"]