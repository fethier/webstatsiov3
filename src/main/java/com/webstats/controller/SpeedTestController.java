package com.webstats.controller;

import com.webstats.dto.SpeedTestRequestDto;
import com.webstats.dto.SpeedTestResponseDto;
import com.webstats.dto.SpeedTestHistoryDto;
import com.webstats.service.SpeedTestService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMethod;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/speedtest")
@CrossOrigin(origins = "*")
public class SpeedTestController {
    
    @Autowired
    private SpeedTestService speedTestService;
    
    @PostMapping("/start")
    public ResponseEntity<?> startSpeedTest(
            @RequestBody(required = false) SpeedTestRequestDto request,
            @RequestParam String userId,
            HttpServletRequest httpRequest) {

        System.out.println("\n=== POST /api/speedtest/start ===");
        System.out.println("Method: " + httpRequest.getMethod());
        System.out.println("Content-Type: " + httpRequest.getContentType());
        System.out.println("UserId: " + userId);
        System.out.println("Request body: " + (request != null ? request.toString() : "null"));
        System.out.println("Headers:");
        httpRequest.getHeaderNames().asIterator().forEachRemaining(header ->
            System.out.println("  " + header + ": " + httpRequest.getHeader(header))
        );

        try {
            if (request == null) {
                return ResponseEntity.badRequest().body("Request body is required");
            }

            // Create mock response
            SpeedTestResponseDto mockResponse = new SpeedTestResponseDto();
            return ResponseEntity.ok(mockResponse);

        } catch (Exception e) {
            System.err.println("Error in startSpeedTest: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    @RequestMapping(value = "/start", method = RequestMethod.OPTIONS)
    public ResponseEntity<?> handleOptions() {
        System.out.println("Received OPTIONS /api/speedtest/start");
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/status/{sessionId}")
    public ResponseEntity<SpeedTestResponseDto> getTestStatus(@PathVariable String sessionId) {
        try {
            SpeedTestResponseDto response = speedTestService.getSessionStatus(sessionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/history")
    public ResponseEntity<List<SpeedTestHistoryDto>> getUserHistory(
            @RequestParam String userId,
            @RequestParam(defaultValue = "10") int limit) {
        
        try {
            List<SpeedTestHistoryDto> history = speedTestService.getUserHistory(userId, limit);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/timeseries")
    public ResponseEntity<SpeedTestHistoryDto.TimeSeriesData> getTimeSeriesData(
            @RequestParam String userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        try {
            SpeedTestHistoryDto.TimeSeriesData data = speedTestService.getTimeSeriesData(userId, startDate, endDate);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Speed Test Service is running");
    }
    
    @GetMapping("/test")
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("Test endpoint working - CORS should allow this");
    }
}