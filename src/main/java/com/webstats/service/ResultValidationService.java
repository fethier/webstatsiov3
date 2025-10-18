package com.webstats.service;

import com.webstats.model.SpeedTestResult;
import org.springframework.stereotype.Service;

@Service
public class ResultValidationService {

    /**
     * Validates speed test results to detect and prevent fake or manipulated results
     */
    public ValidationResult validateSpeedTestResults(SpeedTestResult.SpeedMetrics downloadMetrics,
                                                      SpeedTestResult.SpeedMetrics uploadMetrics,
                                                      SpeedTestResult.LatencyMetrics latencyMetrics) {

        ValidationResult result = new ValidationResult();
        result.setValid(true);

        // Validate download metrics
        if (downloadMetrics != null) {
            if (!validateSpeedMetrics(downloadMetrics, "download")) {
                result.setValid(false);
                result.addWarning("Download metrics appear suspicious");
            }
        }

        // Validate upload metrics
        if (uploadMetrics != null) {
            if (!validateSpeedMetrics(uploadMetrics, "upload")) {
                result.setValid(false);
                result.addWarning("Upload metrics appear suspicious");
            }
        }

        // Validate latency metrics
        if (latencyMetrics != null) {
            if (!validateLatencyMetrics(latencyMetrics)) {
                result.setValid(false);
                result.addWarning("Latency metrics appear suspicious");
            }
        }

        return result;
    }

    private boolean validateSpeedMetrics(SpeedTestResult.SpeedMetrics metrics, String type) {
        // Sanity checks to detect obvious fake results

        // 1. Speed cannot be negative
        if (metrics.getSpeedMbps() < 0 || metrics.getPeakSpeedMbps() < 0 || metrics.getAverageSpeedMbps() < 0) {
            return false;
        }

        // 2. Speed cannot exceed realistic limits (10 Gbps = 10,000 Mbps for consumer connections)
        if (metrics.getSpeedMbps() > 10000 || metrics.getPeakSpeedMbps() > 10000) {
            return false;
        }

        // 3. Peak speed should be >= average speed
        if (metrics.getPeakSpeedMbps() < metrics.getAverageSpeedMbps()) {
            return false;
        }

        // 4. Stability score should be 0-100
        if (metrics.getStabilityScore() < 0 || metrics.getStabilityScore() > 100) {
            return false;
        }

        // 5. Duration should be reasonable (0.1s to 120s)
        if (metrics.getDurationSeconds() < 0.1 || metrics.getDurationSeconds() > 120) {
            return false;
        }

        // 6. Bytes transferred should be consistent with speed and duration
        // Speed (Mbps) * duration (s) * 1024 * 1024 / 8 = bytes
        double expectedBytes = metrics.getSpeedMbps() * metrics.getDurationSeconds() * 1024 * 1024 / 8;
        double tolerance = 0.5; // 50% tolerance for variance
        double minExpectedBytes = expectedBytes * (1 - tolerance);
        double maxExpectedBytes = expectedBytes * (1 + tolerance);

        if (metrics.getBytesTransferred() < minExpectedBytes || metrics.getBytesTransferred() > maxExpectedBytes) {
            // Bytes don't match speed calculation - possible manipulation
            return false;
        }

        // 7. For localhost testing, flag unrealistically high speeds
        // This is a soft warning - might indicate backend-to-backend testing
        if (metrics.getSpeedMbps() > 1000) {
            // Speeds over 1 Gbps are uncommon for consumer connections
            // but we'll allow them with a warning
            System.out.println("WARNING: " + type + " speed over 1 Gbps detected: " + metrics.getSpeedMbps() + " Mbps");
        }

        return true;
    }

    private boolean validateLatencyMetrics(SpeedTestResult.LatencyMetrics metrics) {
        // 1. Ping cannot be negative
        if (metrics.getPingMs() < 0) {
            return false;
        }

        // 2. Ping cannot be unrealistically low (< 0.1ms is suspicious for internet connections)
        if (metrics.getPingMs() < 0.1) {
            return false;
        }

        // 3. Ping cannot be unrealistically high (> 10,000ms = 10 seconds)
        if (metrics.getPingMs() > 10000) {
            return false;
        }

        // 4. Jitter should be non-negative and less than ping
        if (metrics.getJitterMs() < 0 || metrics.getJitterMs() > metrics.getPingMs() * 2) {
            return false;
        }

        // 5. Packet loss should be 0-100%
        if (metrics.getPacketLossPercent() < 0 || metrics.getPacketLossPercent() > 100) {
            return false;
        }

        return true;
    }

    public static class ValidationResult {
        private boolean valid;
        private java.util.List<String> warnings = new java.util.ArrayList<>();

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public java.util.List<String> getWarnings() {
            return warnings;
        }

        public void addWarning(String warning) {
            this.warnings.add(warning);
        }
    }
}
