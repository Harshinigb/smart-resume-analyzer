package com.resumeanalyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(GeminiProperties.class)  // ← add this
public class SmartResumeAnalyzerApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartResumeAnalyzerApplication.class, args);
    }
}