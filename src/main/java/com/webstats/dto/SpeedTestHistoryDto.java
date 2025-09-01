package com.webstats.dto;

import java.time.LocalDateTime;
import java.util.List;

public class SpeedTestHistoryDto {
    
    private String resultId;
    private LocalDateTime testTimestamp;
    private Double downloadSpeedMbps;
    private Double uploadSpeedMbps;
    private Double latencyMs;
    private String location;
    private String serverProvider;
    
    public SpeedTestHistoryDto() {}
    
    public static class TimeSeriesData {
        private List<DataPoint> downloadSeries;
        private List<DataPoint> uploadSeries;
        private List<DataPoint> latencySeries;
        
        public TimeSeriesData() {}
        
        public List<DataPoint> getDownloadSeries() { return downloadSeries; }
        public void setDownloadSeries(List<DataPoint> downloadSeries) { this.downloadSeries = downloadSeries; }
        
        public List<DataPoint> getUploadSeries() { return uploadSeries; }
        public void setUploadSeries(List<DataPoint> uploadSeries) { this.uploadSeries = uploadSeries; }
        
        public List<DataPoint> getLatencySeries() { return latencySeries; }
        public void setLatencySeries(List<DataPoint> latencySeries) { this.latencySeries = latencySeries; }
    }
    
    public static class DataPoint {
        private LocalDateTime timestamp;
        private Double value;
        private String label;
        
        public DataPoint() {}
        
        public DataPoint(LocalDateTime timestamp, Double value) {
            this.timestamp = timestamp;
            this.value = value;
        }
        
        public DataPoint(LocalDateTime timestamp, Double value, String label) {
            this.timestamp = timestamp;
            this.value = value;
            this.label = label;
        }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        
        public Double getValue() { return value; }
        public void setValue(Double value) { this.value = value; }
        
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
    }
    
    // Main getters and setters
    public String getResultId() { return resultId; }
    public void setResultId(String resultId) { this.resultId = resultId; }
    
    public LocalDateTime getTestTimestamp() { return testTimestamp; }
    public void setTestTimestamp(LocalDateTime testTimestamp) { this.testTimestamp = testTimestamp; }
    
    public Double getDownloadSpeedMbps() { return downloadSpeedMbps; }
    public void setDownloadSpeedMbps(Double downloadSpeedMbps) { this.downloadSpeedMbps = downloadSpeedMbps; }
    
    public Double getUploadSpeedMbps() { return uploadSpeedMbps; }
    public void setUploadSpeedMbps(Double uploadSpeedMbps) { this.uploadSpeedMbps = uploadSpeedMbps; }
    
    public Double getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Double latencyMs) { this.latencyMs = latencyMs; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public String getServerProvider() { return serverProvider; }
    public void setServerProvider(String serverProvider) { this.serverProvider = serverProvider; }
}