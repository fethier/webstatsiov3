package com.webstats.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*")
public class TestController {
    
    @GetMapping("/hello")
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok("Hello from Spring Boot API!");
    }
    
    @PostMapping("/echo")
    public ResponseEntity<Map<String, Object>> echo(@RequestBody(required = false) Map<String, Object> payload) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Echo endpoint working");
        response.put("receivedPayload", payload);
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/speedtest-test")
    public ResponseEntity<Map<String, Object>> speedTestTest(
            @RequestBody(required = false) Object payload,
            @RequestParam String userId) {
        
        System.out.println("=== POST /api/test/speedtest-test ===");
        System.out.println("UserId: " + userId);
        System.out.println("Payload: " + payload);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Speed test simulation working");
        response.put("userId", userId);
        response.put("receivedPayload", payload);
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/debug")
    public ResponseEntity<Map<String, Object>> debug() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "Spring Boot is running");
        response.put("activeProfiles", System.getProperty("spring.profiles.active", "default"));
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
}