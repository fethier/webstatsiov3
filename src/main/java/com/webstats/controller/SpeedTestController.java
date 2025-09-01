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

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/speedtest")
@CrossOrigin(origins = "*")
public class SpeedTestController {
    
    @Autowired
    private SpeedTestService speedTestService;
    
    @PostMapping("/start")
    public CompletableFuture<ResponseEntity<SpeedTestResponseDto>> startSpeedTest(
            @Valid @RequestBody SpeedTestRequestDto request,
            @RequestParam String userId,
            HttpServletRequest httpRequest) {
        
        return speedTestService.initiateSpeedTest(request, userId, httpRequest)
                .thenApply(response -> ResponseEntity.ok(response))
                .exceptionally(throwable -> ResponseEntity.badRequest().build());
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
}