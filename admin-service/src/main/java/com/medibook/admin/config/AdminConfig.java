package com.medibook.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Binds the `app.admins` list from application.yml.
 *
 * Each entry becomes an AdminEntry with fullName, email, password.
 * The AdminSeederRunner reads this list at startup and registers
 * any admin that doesn't already exist in the DB.
 */
@Configuration
@ConfigurationProperties(prefix = "app")
@Data
public class AdminConfig {

    private List<AdminEntry> admins;

    @Data
    public static class AdminEntry {
        private String fullName;
        private String email;
        private String password;
    }
}
