package com.webstats.dto;

import com.webstats.model.SpeedTestResult;
import com.webstats.model.SpeedTestSession;

import java.time.LocalDateTime;

public class SpeedTestResponseDto {
    
    private String sessionId;
    private String resultId;
    private LocalDateTime testTimestamp;
    private SpeedTestSession.SessionStatus status;
    private SpeedTestSession.TestPhase currentPhase;
    private Integer progressPercentage;
    private String errorMessage;
    
    private SpeedMetricsDto downloadMetrics;
    private SpeedMetricsDto uploadMetrics;
    private LatencyMetricsDto latencyMetrics;
    private StatisticalSummaryDto statisticalSummary;
    
    public SpeedTestResponseDto() {}
    
    public static class SpeedMetricsDto {
        private Double speedMbps;
        private Long bytesTransferred;
        private Double durationSeconds;
        private Double peakSpeedMbps;
        private Double averageSpeedMbps;
        private Double stabilityScore;
        
        public SpeedMetricsDto() {}
        
        public Double getSpeedMbps() { return speedMbps; }
        public void setSpeedMbps(Double speedMbps) { this.speedMbps = speedMbps; }
        
        public Long getBytesTransferred() { return bytesTransferred; }
        public void setBytesTransferred(Long bytesTransferred) { this.bytesTransferred = bytesTransferred; }
        
        public Double getDurationSeconds() { return durationSeconds; }
        public void setDurationSeconds(Double durationSeconds) { this.durationSeconds = durationSeconds; }
        
        public Double getPeakSpeedMbps() { return peakSpeedMbps; }
        public void setPeakSpeedMbps(Double peakSpeedMbps) { this.peakSpeedMbps = peakSpeedMbps; }
        
        public Double getAverageSpeedMbps() { return averageSpeedMbps; }
        public void setAverageSpeedMbps(Double averageSpeedMbps) { this.averageSpeedMbps = averageSpeedMbps; }
        
        public Double getStabilityScore() { return stabilityScore; }
        public void setStabilityScore(Double stabilityScore) { this.stabilityScore = stabilityScore; }
    }
    
    public static class LatencyMetricsDto {
        private Double pingMs;
        private Double jitterMs;
        private Double packetLossPercent;
        private Double dnsLookupMs;
        private Double tcpConnectMs;
        private Double sslHandshakeMs;
        private Double firstByteMs;
        
        public LatencyMetricsDto() {}
        
        public Double getPingMs() { return pingMs; }
        public void setPingMs(Double pingMs) { this.pingMs = pingMs; }
        
        public Double getJitterMs() { return jitterMs; }
        public void setJitterMs(Double jitterMs) { this.jitterMs = jitterMs; }
        
        public Double getPacketLossPercent() { return packetLossPercent; }
        public void setPacketLossPercent(Double packetLossPercent) { this.packetLossPercent = packetLossPercent; }
        
        public Double getDnsLookupMs() { return dnsLookupMs; }
        public void setDnsLookupMs(Double dnsLookupMs) { this.dnsLookupMs = dnsLookupMs; }
        
        public Double getTcpConnectMs() { return tcpConnectMs; }
        public void setTcpConnectMs(Double tcpConnectMs) { this.tcpConnectMs = tcpConnectMs; }
        
        public Double getSslHandshakeMs() { return sslHandshakeMs; }
        public void setSslHandshakeMs(Double sslHandshakeMs) { this.sslHandshakeMs = sslHandshakeMs; }
        
        public Double getFirstByteMs() { return firstByteMs; }
        public void setFirstByteMs(Double firstByteMs) { this.firstByteMs = firstByteMs; }
    }
    
    public static class StatisticalSummaryDto {
        private StatisticsDto downloadStats;
        private StatisticsDto uploadStats;
        private StatisticsDto latencyStats;
        
        public StatisticalSummaryDto() {}
        
        public StatisticsDto getDownloadStats() { return downloadStats; }
        public void setDownloadStats(StatisticsDto downloadStats) { this.downloadStats = downloadStats; }
        
        public StatisticsDto getUploadStats() { return uploadStats; }
        public void setUploadStats(StatisticsDto uploadStats) { this.uploadStats = uploadStats; }
        
        public StatisticsDto getLatencyStats() { return latencyStats; }
        public void setLatencyStats(StatisticsDto latencyStats) { this.latencyStats = latencyStats; }
    }
    
    public static class StatisticsDto {
        private Double median;
        private Double mean;
        private Double min;
        private Double max;
        private Double percentile95;
        private Double percentile99;
        private Double standardDeviation;
        
        public StatisticsDto() {}
        
        public Double getMedian() { return median; }
        public void setMedian(Double median) { this.median = median; }
        
        public Double getMean() { return mean; }
        public void setMean(Double mean) { this.mean = mean; }
        
        public Double getMin() { return min; }
        public void setMin(Double min) { this.min = min; }
        
        public Double getMax() { return max; }
        public void setMax(Double max) { this.max = max; }
        
        public Double getPercentile95() { return percentile95; }
        public void setPercentile95(Double percentile95) { this.percentile95 = percentile95; }
        
        public Double getPercentile99() { return percentile99; }
        public void setPercentile99(Double percentile99) { this.percentile99 = percentile99; }
        
        public Double getStandardDeviation() { return standardDeviation; }
        public void setStandardDeviation(Double standardDeviation) { this.standardDeviation = standardDeviation; }
    }
    
    // Main getters and setters
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getResultId() { return resultId; }
    public void setResultId(String resultId) { this.resultId = resultId; }
    
    public LocalDateTime getTestTimestamp() { return testTimestamp; }
    public void setTestTimestamp(LocalDateTime testTimestamp) { this.testTimestamp = testTimestamp; }
    
    public SpeedTestSession.SessionStatus getStatus() { return status; }
    public void setStatus(SpeedTestSession.SessionStatus status) { this.status = status; }
    
    public SpeedTestSession.TestPhase getCurrentPhase() { return currentPhase; }
    public void setCurrentPhase(SpeedTestSession.TestPhase currentPhase) { this.currentPhase = currentPhase; }
    
    public Integer getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(Integer progressPercentage) { this.progressPercentage = progressPercentage; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public SpeedMetricsDto getDownloadMetrics() { return downloadMetrics; }
    public void setDownloadMetrics(SpeedMetricsDto downloadMetrics) { this.downloadMetrics = downloadMetrics; }
    
    public SpeedMetricsDto getUploadMetrics() { return uploadMetrics; }
    public void setUploadMetrics(SpeedMetricsDto uploadMetrics) { this.uploadMetrics = uploadMetrics; }
    
    public LatencyMetricsDto getLatencyMetrics() { return latencyMetrics; }
    public void setLatencyMetrics(LatencyMetricsDto latencyMetrics) { this.latencyMetrics = latencyMetrics; }
    
    public StatisticalSummaryDto getStatisticalSummary() { return statisticalSummary; }
    public void setStatisticalSummary(StatisticalSummaryDto statisticalSummary) { this.statisticalSummary = statisticalSummary; }
}