package com.example.multitenantdocumentsearch;

import java.util.Map;
import java.util.TimeZone;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Main entry point for the Multi-Tenant Document Search Spring Boot application.
 */
@SpringBootApplication
public class MultiTenantDocumentSearchApplication {
    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    public static void main(String[] args) {
        // Force JVM timezone to Asia/Kolkata
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        System.out.println("[DEBUG] JVM Default TimeZone: " + TimeZone.getDefault().getID());
        SpringApplication.run(MultiTenantDocumentSearchApplication.class, args);
    }

    @Bean
    public CommandLineRunner printEnvAndJdbcUrl() {
        return args -> {
            System.out.println("[DEBUG] spring.datasource.url: " + datasourceUrl);
            System.out.println("[DEBUG] Environment Variables:");
            for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
                if (entry.getKey().toLowerCase().contains("zone") || entry.getValue().toLowerCase().contains("calcutta")) {
                    System.out.println(entry.getKey() + "=" + entry.getValue());
                }
            }
        };
    }
}
