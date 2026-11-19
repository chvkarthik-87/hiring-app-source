package com.hiring.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class HiringAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(HiringAppApplication.class, args);
    }

    @GetMapping("/")
    public String home() {
        return "Hiring App is running successfully!";
    }

    @GetMapping("/health")
    public String health() {
        return "UP";
    }
}
