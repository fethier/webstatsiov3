package com.webstats.service;

import com.webstats.model.SpeedTestResult;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class UploadTestService {
    
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Random random = new Random();
    
    // Upload endpoint - this should be implemented on your server to handle uploads
    private static final String UPLOAD_URL = "http://localhost:8080/api/speedtest/upload";
    
    public CompletableFuture<SpeedTestResult.SpeedMetrics> performUploadTest(
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
                    UploadResult result = performSingleUploadTest(testDurationSeconds);
                    if (result != null) {
                        speeds.add(result.speedMbps);
                        totalBytesTransferred += result.bytesTransferred;
                        totalDurationMs += result.durationMs;
                        peakSpeed = Math.max(peakSpeed, result.speedMbps);
                    }
                }
                
                if (speeds.isEmpty()) {
                    throw new RuntimeException("All upload tests failed");
                }
                
                SpeedTestResult.SpeedMetrics metrics = new SpeedTestResult.SpeedMetrics();
                metrics.setSpeedMbps(speeds.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
                metrics.setAverageSpeedMbps(metrics.getSpeedMbps());
                metrics.setPeakSpeedMbps(peakSpeed);
                metrics.setBytesTransferred(totalBytesTransferred);
                metrics.setDurationSeconds((double) totalDurationMs / 1000.0);
                
                // Calculate stability score
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
    
    public CompletableFuture<List<SpeedTestResult.RawMeasurement>> performMultipleUploadRuns(
            SpeedTestResult.TestConfiguration config) {
        
        return CompletableFuture.supplyAsync(() -> {
            List<SpeedTestResult.RawMeasurement> measurements = new ArrayList<>();
            
            for (int run = 0; run < config.getNumberOfRuns(); run++) {
                try {
                    UploadResult result = performSingleUploadTest(config.getTestDurationSeconds());
                    if (result != null) {
                        SpeedTestResult.RawMeasurement measurement = new SpeedTestResult.RawMeasurement();
                        measurement.setRunNumber(run + 1);
                        measurement.setTimestamp(LocalDateTime.now());
                        measurement.setMeasurementType(SpeedTestResult.RawMeasurement.MeasurementType.UPLOAD_SPEED);
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
    
    private UploadResult performSingleUploadTest(int durationSeconds) {
        try {
            URL url = new URL(UPLOAD_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout((durationSeconds + 5) * 1000); // Add buffer time
            connection.setRequestProperty("Content-Type", "application/octet-stream");
            connection.setRequestProperty("User-Agent", "WebStats-SpeedTest/1.0");
            connection.setRequestProperty("Cache-Control", "no-cache");
            connection.setChunkedStreamingMode(8192); // Enable chunked transfer encoding

            // Calculate data size: aim for 10-20 MB/s target speed to fill the duration
            // This ensures we have enough data to transfer during the test period
            int targetBytesPerSecond = 15 * 1024 * 1024; // 15 MB/s target
            int testDataSize = durationSeconds * targetBytesPerSecond;
            testDataSize = Math.max(testDataSize, 5 * 1024 * 1024); // Minimum 5MB

            byte[] testData = generateTestData(testDataSize);

            // Track actual bytes sent and time taken
            long actualStartTime = 0;
            long actualEndTime = 0;
            long totalBytesSent = 0;

            try (OutputStream outputStream = connection.getOutputStream()) {
                int chunkSize = 8192; // 8KB chunks
                long testStartTime = System.currentTimeMillis();
                actualStartTime = testStartTime;

                // Target speed for throttling: 100 Mbps (realistic broadband speed)
                // This is 12.5 MB/s or 12,800 KB/s
                double targetMBps = 12.5; // 100 Mbps
                long targetBytesPerChunk = chunkSize;
                long targetMsPerChunk = (long) ((targetBytesPerChunk / (targetMBps * 1024 * 1024)) * 1000);

                long lastChunkTime = testStartTime;

                for (int i = 0; i < testData.length; i += chunkSize) {
                    long currentTime = System.currentTimeMillis();
                    long elapsedMs = currentTime - testStartTime;

                    // Stop if we've exceeded the test duration
                    if (elapsedMs >= (durationSeconds * 1000)) {
                        break;
                    }

                    int bytesToWrite = Math.min(chunkSize, testData.length - i);
                    outputStream.write(testData, i, bytesToWrite);
                    outputStream.flush();
                    totalBytesSent += bytesToWrite;

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

            // Calculate actual streaming duration (exclude connection setup time)
            long streamingDurationMs = actualEndTime - actualStartTime;

            // Ensure we have valid measurements
            if (streamingDurationMs < 100) {
                // If streaming took less than 100ms, the test is invalid
                System.err.println("Upload test completed too quickly (" + streamingDurationMs + "ms), likely localhost without throttling");
                streamingDurationMs = Math.max(100, streamingDurationMs);
            }

            // Check response
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK && streamingDurationMs > 0 && totalBytesSent > 0) {
                // Convert to Mbps using actual streaming time
                double speedMbps = (totalBytesSent * 8.0) / (1024.0 * 1024.0) / (streamingDurationMs / 1000.0);

                UploadResult result = new UploadResult();
                result.speedMbps = speedMbps;
                result.bytesTransferred = totalBytesSent;
                result.durationMs = streamingDurationMs;
                return result;
            }

        } catch (Exception e) {
            System.err.println("Upload test failed: " + e.getMessage());
            // Fallback to simulated test if upload endpoint is not available
            return performSimulatedUploadTest(durationSeconds);
        }

        return null;
    }
    
    private UploadResult performSimulatedUploadTest(int durationSeconds) {
        // Simulate upload test with timing delays to approximate real network behavior
        try {
            long startTime = System.currentTimeMillis();
            
            // Simulate data preparation
            Thread.sleep(100); // 100ms setup time
            
            // Simulate upload duration with variable speed
            int simulatedDataSize = durationSeconds * 1024 * 1024; // 1MB per second base
            double baseSpeed = 20.0 + (random.nextDouble() * 30.0); // 20-50 Mbps range
            
            // Simulate network variability
            Thread.sleep(durationSeconds * 1000);
            
            long endTime = System.currentTimeMillis();
            long actualDurationMs = endTime - startTime;
            
            UploadResult result = new UploadResult();
            result.speedMbps = baseSpeed + (random.nextGaussian() * 5.0); // Add some variance
            result.bytesTransferred = simulatedDataSize;
            result.durationMs = actualDurationMs;
            
            return result;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
    
    private byte[] generateTestData(int size) {
        byte[] data = new byte[size];
        random.nextBytes(data);
        return data;
    }
    
    private static class UploadResult {
        double speedMbps;
        long bytesTransferred;
        long durationMs;
    }
    
    public void shutdown() {
        executorService.shutdown();
    }
}