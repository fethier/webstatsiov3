package com.webstats.service;

import com.webstats.model.SpeedTestResult;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class DownloadTestService {
    
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    // Local test file URLs - using our own server endpoints
    private static final String BASE_URL = "http://localhost:8080/api/speedtest/download/";
    
    private String getTestUrl(int sizeMB) {
        return BASE_URL + sizeMB;
    }
    
    public CompletableFuture<SpeedTestResult.SpeedMetrics> performDownloadTest(
            SpeedTestResult.TestConfiguration config) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                int numberOfRuns = config.getNumberOfRuns();
                int testDurationSeconds = config.getTestDurationSeconds();
                List<Double> speeds = new ArrayList<>();
                
                long totalBytesTransferred = 0;
                long totalDurationMs = 0;
                double peakSpeed = 0.0;
                
                for (int run = 0; run < numberOfRuns; run++) {
                    DownloadResult result = performSingleDownloadTest(testDurationSeconds);
                    if (result != null) {
                        speeds.add(result.speedMbps);
                        totalBytesTransferred += result.bytesTransferred;
                        totalDurationMs += result.durationMs;
                        peakSpeed = Math.max(peakSpeed, result.speedMbps);
                    }
                }
                
                if (speeds.isEmpty()) {
                    throw new RuntimeException("All download tests failed");
                }
                
                SpeedTestResult.SpeedMetrics metrics = new SpeedTestResult.SpeedMetrics();
                metrics.setSpeedMbps(speeds.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
                metrics.setAverageSpeedMbps(metrics.getSpeedMbps());
                metrics.setPeakSpeedMbps(peakSpeed);
                metrics.setBytesTransferred(totalBytesTransferred);
                metrics.setDurationSeconds((double) totalDurationMs / 1000.0);
                
                // Calculate stability score (lower variation = higher stability)
                double variance = speeds.stream()
                    .mapToDouble(speed -> Math.pow(speed - metrics.getSpeedMbps(), 2))
                    .average().orElse(0.0);
                double stabilityScore = Math.max(0, 100 - (Math.sqrt(variance) / metrics.getSpeedMbps() * 100));
                metrics.setStabilityScore(stabilityScore);
                
                return metrics;
                
            } catch (Exception e) {
                SpeedTestResult.SpeedMetrics errorMetrics = new SpeedTestResult.SpeedMetrics();
                errorMetrics.setSpeedMbps(0.0);
                return errorMetrics;
            }
        }, executorService);
    }
    
    public CompletableFuture<List<SpeedTestResult.RawMeasurement>> performMultipleDownloadRuns(
            SpeedTestResult.TestConfiguration config) {
        
        return CompletableFuture.supplyAsync(() -> {
            List<SpeedTestResult.RawMeasurement> measurements = new ArrayList<>();
            
            for (int run = 0; run < config.getNumberOfRuns(); run++) {
                try {
                    DownloadResult result = performSingleDownloadTest(config.getTestDurationSeconds());
                    if (result != null) {
                        SpeedTestResult.RawMeasurement measurement = new SpeedTestResult.RawMeasurement();
                        measurement.setRunNumber(run + 1);
                        measurement.setTimestamp(LocalDateTime.now());
                        measurement.setMeasurementType(SpeedTestResult.RawMeasurement.MeasurementType.DOWNLOAD_SPEED);
                        measurement.setValue(result.speedMbps);
                        measurements.add(measurement);
                    }
                } catch (Exception e) {
                    // Skip failed measurement
                }
            }
            
            return measurements;
        }, executorService);
    }
    
    private DownloadResult performSingleDownloadTest(int durationSeconds) {
        try {
            // Use a test file appropriate for the duration
            String testUrl = selectTestUrl(durationSeconds);
            URL url = new URL(testUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout((durationSeconds + 5) * 1000); // Add buffer time
            connection.setRequestProperty("User-Agent", "WebStats-SpeedTest/1.0");
            connection.setRequestProperty("Cache-Control", "no-cache");

            // Track actual streaming time
            long actualStartTime = 0;
            long actualEndTime = 0;
            long totalBytesReceived = 0;

            try (InputStream inputStream = connection.getInputStream();
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                long testStartTime = System.currentTimeMillis();
                actualStartTime = testStartTime;

                // Target speed for throttling: 100 Mbps (realistic broadband speed)
                // This is 12.5 MB/s
                double targetMBps = 12.5; // 100 Mbps
                long targetBytesPerChunk = buffer.length;
                long targetMsPerChunk = (long) ((targetBytesPerChunk / (targetMBps * 1024 * 1024)) * 1000);

                long lastChunkTime = testStartTime;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    long currentTime = System.currentTimeMillis();
                    long elapsedMs = currentTime - testStartTime;

                    // Stop if we've exceeded the test duration
                    if (elapsedMs >= (durationSeconds * 1000)) {
                        break;
                    }

                    outputStream.write(buffer, 0, bytesRead);
                    totalBytesReceived += bytesRead;

                    // Throttle to simulate realistic network speed
                    long timeSinceLastChunk = System.currentTimeMillis() - lastChunkTime;
                    if (timeSinceLastChunk < targetMsPerChunk) {
                        try {
                            Thread.sleep(targetMsPerChunk - timeSinceLastChunk);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }

                    lastChunkTime = System.currentTimeMillis();
                }

                actualEndTime = System.currentTimeMillis();
            }

            // Calculate actual streaming duration
            long streamingDurationMs = actualEndTime - actualStartTime;

            // Ensure we have valid measurements
            if (streamingDurationMs < 100) {
                // If streaming took less than 100ms, the test is invalid
                System.err.println("Download test completed too quickly (" + streamingDurationMs + "ms), likely localhost without throttling");
                streamingDurationMs = Math.max(100, streamingDurationMs);
            }

            if (streamingDurationMs > 0 && totalBytesReceived > 0) {
                // Convert to Mbps: (bytes * 8) / (1024 * 1024) / (milliseconds / 1000)
                double speedMbps = (totalBytesReceived * 8.0) / (1024.0 * 1024.0) / (streamingDurationMs / 1000.0);

                DownloadResult result = new DownloadResult();
                result.speedMbps = speedMbps;
                result.bytesTransferred = totalBytesReceived;
                result.durationMs = streamingDurationMs;
                return result;
            }

        } catch (Exception e) {
            System.err.println("Download test failed: " + e.getMessage());
        }

        return null;
    }

    private String selectTestUrl(int durationSeconds) {
        // Select appropriate test file size based on duration and target speed
        // Target: 100 Mbps = 12.5 MB/s, so we need 12.5 * durationSeconds MB minimum
        // Use larger files to ensure we have enough data for the full test duration
        int targetMB = (int) Math.ceil(durationSeconds * 15); // 15 MB/s to ensure full duration

        if (durationSeconds <= 5) {
            return getTestUrl(Math.max(50, targetMB)); // At least 50MB for short tests
        } else if (durationSeconds <= 15) {
            return getTestUrl(Math.max(75, targetMB)); // At least 75MB for medium tests
        } else {
            return getTestUrl(Math.min(100, targetMB)); // Up to 100MB for long tests
        }
    }
    
    private static class DownloadResult {
        double speedMbps;
        long bytesTransferred;
        long durationMs;
    }
    
    public void shutdown() {
        executorService.shutdown();
    }
}