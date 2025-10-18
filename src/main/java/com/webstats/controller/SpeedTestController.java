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
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/speedtest")
@CrossOrigin(origins = "*")
public class SpeedTestController {
    
    @Autowired
    private SpeedTestService speedTestService;

    @Autowired
    private com.webstats.service.ResultValidationService validationService;

    @Autowired
    private com.webstats.repository.SpeedTestResultRepository resultRepository;
    
    @PostMapping("/start")
    public ResponseEntity<?> startSpeedTest(
            @RequestBody(required = false) SpeedTestRequestDto request,
            @RequestParam(required = false) String userId,
            HttpServletRequest httpRequest) {

        // Use anonymous if no userId provided
        if (userId == null || userId.isEmpty()) {
            userId = "anonymous";
        }

        System.out.println("\n=== POST /api/speedtest/start ===");
        System.out.println("Method: " + httpRequest.getMethod());
        System.out.println("Content-Type: " + httpRequest.getContentType());
        System.out.println("UserId: " + userId + (userId.equals("anonymous") ? " (anonymous user)" : ""));
        System.out.println("Request body: " + (request != null ? request.toString() : "null"));
        System.out.println("Headers:");
        httpRequest.getHeaderNames().asIterator().forEachRemaining(header ->
            System.out.println("  " + header + ": " + httpRequest.getHeader(header))
        );

        try {
            if (request == null) {
                return ResponseEntity.badRequest().body("Request body is required");
            }

            // Actually call the service to initiate speed test
            SpeedTestResponseDto response = speedTestService.initiateSpeedTest(request, userId, httpRequest).get();
            return ResponseEntity.ok(response);

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
            @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = "10") int limit) {
        
        try {
            if (userId == null || userId.isEmpty()) {
                userId = "anonymous";
            }
            List<SpeedTestHistoryDto> history = speedTestService.getUserHistory(userId, limit);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/timeseries")
    public ResponseEntity<SpeedTestHistoryDto.TimeSeriesData> getTimeSeriesData(
            @RequestParam(required = false) String userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        try {
            if (userId == null || userId.isEmpty()) {
                userId = "anonymous";
            }
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
    
    @PostMapping("/upload")
    public ResponseEntity<String> handleUploadTest(HttpServletRequest request) {
        try {
            long startTime = System.currentTimeMillis();
            long totalBytesReceived = 0;

            // Read the input stream in chunks to simulate streaming
            byte[] buffer = new byte[8192];
            int bytesRead;

            // Read from request input stream
            try (var inputStream = request.getInputStream()) {
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    totalBytesReceived += bytesRead;

                    // Add small delay to simulate network processing
                    // This helps prevent instant completion on localhost
                    if (totalBytesReceived % (1024 * 1024) == 0) { // Every 1MB
                        Thread.sleep(1);
                    }
                }
            }

            long endTime = System.currentTimeMillis();
            long durationMs = endTime - startTime;

            // Calculate speed for logging
            double speedMbps = 0;
            if (durationMs > 0) {
                speedMbps = (totalBytesReceived * 8.0) / (1024.0 * 1024.0) / (durationMs / 1000.0);
            }

            String response = String.format(
                "Upload test completed. Received %d bytes in %d ms (%.2f Mbps)",
                totalBytesReceived, durationMs, speedMbps
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Upload test failed: " + e.getMessage());
        }
    }
    
    @PostMapping("/results")
    public ResponseEntity<?> saveClientSideResults(
            @RequestBody SpeedTestResponseDto clientResults,
            @RequestParam(required = false) String userId,
            HttpServletRequest httpRequest) {

        System.out.println("\n=== POST /api/speedtest/results ===");
        System.out.println("Received client-side test results for validation and storage");

        try {
            if (userId == null || userId.isEmpty()) {
                userId = "anonymous";
            }

            // Convert DTOs to model objects for validation
            com.webstats.model.SpeedTestResult.SpeedMetrics downloadMetrics = null;
            if (clientResults.getDownloadMetrics() != null) {
                downloadMetrics = new com.webstats.model.SpeedTestResult.SpeedMetrics();
                downloadMetrics.setSpeedMbps(clientResults.getDownloadMetrics().getSpeedMbps());
                downloadMetrics.setBytesTransferred(clientResults.getDownloadMetrics().getBytesTransferred());
                downloadMetrics.setDurationSeconds(clientResults.getDownloadMetrics().getDurationSeconds());
                downloadMetrics.setPeakSpeedMbps(clientResults.getDownloadMetrics().getPeakSpeedMbps());
                downloadMetrics.setAverageSpeedMbps(clientResults.getDownloadMetrics().getAverageSpeedMbps());
                downloadMetrics.setStabilityScore(clientResults.getDownloadMetrics().getStabilityScore());
            }

            com.webstats.model.SpeedTestResult.SpeedMetrics uploadMetrics = null;
            if (clientResults.getUploadMetrics() != null) {
                uploadMetrics = new com.webstats.model.SpeedTestResult.SpeedMetrics();
                uploadMetrics.setSpeedMbps(clientResults.getUploadMetrics().getSpeedMbps());
                uploadMetrics.setBytesTransferred(clientResults.getUploadMetrics().getBytesTransferred());
                uploadMetrics.setDurationSeconds(clientResults.getUploadMetrics().getDurationSeconds());
                uploadMetrics.setPeakSpeedMbps(clientResults.getUploadMetrics().getPeakSpeedMbps());
                uploadMetrics.setAverageSpeedMbps(clientResults.getUploadMetrics().getAverageSpeedMbps());
                uploadMetrics.setStabilityScore(clientResults.getUploadMetrics().getStabilityScore());
            }

            com.webstats.model.SpeedTestResult.LatencyMetrics latencyMetrics = null;
            if (clientResults.getLatencyMetrics() != null) {
                latencyMetrics = new com.webstats.model.SpeedTestResult.LatencyMetrics();
                latencyMetrics.setPingMs(clientResults.getLatencyMetrics().getPingMs());
                latencyMetrics.setJitterMs(clientResults.getLatencyMetrics().getJitterMs());
                latencyMetrics.setPacketLossPercent(clientResults.getLatencyMetrics().getPacketLossPercent());
                latencyMetrics.setDnsLookupMs(clientResults.getLatencyMetrics().getDnsLookupMs());
                latencyMetrics.setTcpConnectMs(clientResults.getLatencyMetrics().getTcpConnectMs());
                latencyMetrics.setSslHandshakeMs(clientResults.getLatencyMetrics().getSslHandshakeMs());
                latencyMetrics.setFirstByteMs(clientResults.getLatencyMetrics().getFirstByteMs());
            }

            // Validate results
            com.webstats.service.ResultValidationService.ValidationResult validation =
                    validationService.validateSpeedTestResults(downloadMetrics, uploadMetrics, latencyMetrics);

            if (!validation.isValid()) {
                System.out.println("WARNING: Client results failed validation: " + validation.getWarnings());
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Results failed validation",
                        "warnings", validation.getWarnings()
                ));
            }

            // Create and save result
            com.webstats.model.SpeedTestResult result = new com.webstats.model.SpeedTestResult();
            result.setUserId(userId);
            result.setOrganizationId("public");
            result.setSessionId(clientResults.getSessionId());
            result.setDownloadMetrics(downloadMetrics);
            result.setUploadMetrics(uploadMetrics);
            result.setLatencyMetrics(latencyMetrics);

            // Set client info
            com.webstats.model.SpeedTestResult.ClientInfo clientInfo = new com.webstats.model.SpeedTestResult.ClientInfo();
            clientInfo.setIpAddress(getClientIpAddress(httpRequest));
            clientInfo.setUserAgent(httpRequest.getHeader("User-Agent"));
            result.setClientInfo(clientInfo);

            // Set server info
            com.webstats.model.SpeedTestResult.ServerInfo serverInfo = new com.webstats.model.SpeedTestResult.ServerInfo();
            serverInfo.setServerId("default-server-1");
            serverInfo.setLocation("Default Location");
            serverInfo.setProvider("WebStats.io");
            result.setServerInfo(serverInfo);

            // Save to MongoDB
            result = resultRepository.save(result);

            System.out.println("Results saved successfully with ID: " + result.getId());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "resultId", result.getId(),
                    "message", "Results validated and saved successfully",
                    "warnings", validation.getWarnings()
            ));

        } catch (Exception e) {
            System.err.println("Error saving client results: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to save results: " + e.getMessage()
            ));
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    @GetMapping("/analytics/summary")
    public ResponseEntity<Object> getAnalyticsSummary(@RequestParam(required = false) String userId) {
        try {
            if (userId == null || userId.isEmpty()) {
                userId = "anonymous";
            }

            // Get basic analytics data for the user
            Object summary = speedTestService.getAnalyticsSummary(userId);
            return ResponseEntity.ok(summary);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to get analytics summary: " + e.getMessage());
        }
    }
    
    @GetMapping("/download/{sizeMB}")
    public ResponseEntity<byte[]> downloadTestFile(@PathVariable int sizeMB) {
        try {
            // Limit size to prevent abuse
            if (sizeMB < 1 || sizeMB > 100) {
                return ResponseEntity.badRequest().build();
            }
            
            // Generate test data
            int sizeBytes = sizeMB * 1024 * 1024;
            byte[] testData = new byte[sizeBytes];
            
            // Fill with pseudo-random data (for better compression testing)
            for (int i = 0; i < sizeBytes; i++) {
                testData[i] = (byte) (i % 256);
            }
            
            return ResponseEntity.ok()
                .header("Content-Type", "application/octet-stream")
                .header("Content-Length", String.valueOf(sizeBytes))
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .body(testData);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}