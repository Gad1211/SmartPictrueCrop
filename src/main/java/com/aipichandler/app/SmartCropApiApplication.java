package com.aipichandler.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Optional API entrypoint. Desktop EXE still uses MainApp.
 */
@SpringBootApplication(scanBasePackages = "com.aipichandler")
public class SmartCropApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartCropApiApplication.class, args);
    }
}
