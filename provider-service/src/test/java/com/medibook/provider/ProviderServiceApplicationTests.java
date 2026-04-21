package com.medibook.provider;
 
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
 
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "medibook.jwt.secret=test-secret-key-must-be-at-least-256-bits-long-for-testing",
        "eureka.client.enabled=false"
})
class ProviderServiceApplicationTests {
 
    @Test
    void contextLoads() {
    }
}