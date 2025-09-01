package com.webstats.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collection = "speed_test_results")
public class SpeedTestResult {
    
    @Id
    private String id;
    
    @Field("user_id")
    private String userId;
    
    @Field("organization_id")
    private String organizationId;
    
    @Field("session_id")
    private String sessionId;
    
    @NotNull
    @Field("test_timestamp")
    private LocalDateTime testTimestamp;
    
    @Field("client_info")
    private ClientInfo clientInfo;
    
    @Field("server_info")
    private ServerInfo serverInfo;
    
    @Field("download_metrics")
    private SpeedMetrics downloadMetrics;
    
    @Field("upload_metrics")
    private SpeedMetrics uploadMetrics;
    
    @Field("latency_metrics")
    private LatencyMetrics latencyMetrics;
    
    @Field("test_configuration")
    private TestConfiguration testConfiguration;
    
    @Field("statistical_summary")
    private StatisticalSummary statisticalSummary;
    
    @Field("raw_measurements")
    private List<RawMeasurement> rawMeasurements;
    
    public SpeedTestResult() {
        this.testTimestamp = LocalDateTime.now();
    }
    
    public static class ClientInfo {
        @Field("ip_address")
        private String ipAddress;
        
        @Field("user_agent")
        private String userAgent;
        
        @Field("geolocation")
        private Geolocation geolocation;
        
        @Field("connection_type")
        private String connectionType;
        
        public ClientInfo() {}
        
        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
        
        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
        
        public Geolocation getGeolocation() { return geolocation; }
        public void setGeolocation(Geolocation geolocation) { this.geolocation = geolocation; }
        
        public String getConnectionType() { return connectionType; }
        public void setConnectionType(String connectionType) { this.connectionType = connectionType; }
    }
    
    public static class Geolocation {
        @Field("country")
        private String country;
        
        @Field("city")
        private String city;
        
        @Field("latitude")
        private Double latitude;
        
        @Field("longitude")
        private Double longitude;
        
        public Geolocation() {}
        
        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }
        
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        
        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }
        
        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }
    }
    
    public static class ServerInfo {
        @Field("server_id")
        private String serverId;
        
        @Field("location")
        private String location;
        
        @Field("provider")
        private String provider;
        
        public ServerInfo() {}
        
        public String getServerId() { return serverId; }
        public void setServerId(String serverId) { this.serverId = serverId; }
        
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
    }
    
    public static class SpeedMetrics {
        @Field("speed_mbps")
        private Double speedMbps;
        
        @Field("bytes_transferred")
        private Long bytesTransferred;
        
        @Field("duration_seconds")
        private Double durationSeconds;
        
        @Field("peak_speed_mbps")
        private Double peakSpeedMbps;
        
        @Field("average_speed_mbps")
        private Double averageSpeedMbps;
        
        @Field("stability_score")
        private Double stabilityScore;
        
        public SpeedMetrics() {}
        
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
    
    public static class LatencyMetrics {
        @Field("ping_ms")
        private Double pingMs;
        
        @Field("jitter_ms")
        private Double jitterMs;
        
        @Field("packet_loss_percent")
        private Double packetLossPercent;
        
        @Field("dns_lookup_ms")
        private Double dnsLookupMs;
        
        @Field("tcp_connect_ms")
        private Double tcpConnectMs;
        
        @Field("ssl_handshake_ms")
        private Double sslHandshakeMs;
        
        @Field("first_byte_ms")
        private Double firstByteMs;
        
        public LatencyMetrics() {}
        
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
    
    public static class TestConfiguration {
        @Field("test_type")
        private TestType testType;
        
        @Field("test_duration_seconds")
        private Integer testDurationSeconds;
        
        @Field("number_of_runs")
        private Integer numberOfRuns;
        
        @Field("concurrent_connections")
        private Integer concurrentConnections;
        
        @Field("test_file_size_mb")
        private Double testFileSizeMb;
        
        public TestConfiguration() {
            this.testType = TestType.FULL;
            this.testDurationSeconds = 10;
            this.numberOfRuns = 3;
            this.concurrentConnections = 4;
        }
        
        public enum TestType {
            DOWNLOAD_ONLY, UPLOAD_ONLY, LATENCY_ONLY, FULL
        }
        
        public TestType getTestType() { return testType; }
        public void setTestType(TestType testType) { this.testType = testType; }
        
        public Integer getTestDurationSeconds() { return testDurationSeconds; }
        public void setTestDurationSeconds(Integer testDurationSeconds) { this.testDurationSeconds = testDurationSeconds; }
        
        public Integer getNumberOfRuns() { return numberOfRuns; }
        public void setNumberOfRuns(Integer numberOfRuns) { this.numberOfRuns = numberOfRuns; }
        
        public Integer getConcurrentConnections() { return concurrentConnections; }
        public void setConcurrentConnections(Integer concurrentConnections) { this.concurrentConnections = concurrentConnections; }
        
        public Double getTestFileSizeMb() { return testFileSizeMb; }
        public void setTestFileSizeMb(Double testFileSizeMb) { this.testFileSizeMb = testFileSizeMb; }
    }
    
    public static class StatisticalSummary {
        @Field("download_stats")
        private Statistics downloadStats;
        
        @Field("upload_stats")
        private Statistics uploadStats;
        
        @Field("latency_stats")
        private Statistics latencyStats;
        
        public StatisticalSummary() {}
        
        public Statistics getDownloadStats() { return downloadStats; }
        public void setDownloadStats(Statistics downloadStats) { this.downloadStats = downloadStats; }
        
        public Statistics getUploadStats() { return uploadStats; }
        public void setUploadStats(Statistics uploadStats) { this.uploadStats = uploadStats; }
        
        public Statistics getLatencyStats() { return latencyStats; }
        public void setLatencyStats(Statistics latencyStats) { this.latencyStats = latencyStats; }
    }
    
    public static class Statistics {
        @Field("median")
        private Double median;
        
        @Field("mean")
        private Double mean;
        
        @Field("min")
        private Double min;
        
        @Field("max")
        private Double max;
        
        @Field("percentile_95")
        private Double percentile95;
        
        @Field("percentile_99")
        private Double percentile99;
        
        @Field("standard_deviation")
        private Double standardDeviation;
        
        public Statistics() {}
        
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
    
    public static class RawMeasurement {
        @Field("run_number")
        private Integer runNumber;
        
        @Field("timestamp")
        private LocalDateTime timestamp;
        
        @Field("measurement_type")
        private MeasurementType measurementType;
        
        @Field("value")
        private Double value;
        
        @Field("metadata")
        private Map<String, Object> metadata;
        
        public enum MeasurementType {
            DOWNLOAD_SPEED, UPLOAD_SPEED, LATENCY, JITTER, PACKET_LOSS
        }
        
        public RawMeasurement() {}
        
        public Integer getRunNumber() { return runNumber; }
        public void setRunNumber(Integer runNumber) { this.runNumber = runNumber; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        
        public MeasurementType getMeasurementType() { return measurementType; }
        public void setMeasurementType(MeasurementType measurementType) { this.measurementType = measurementType; }
        
        public Double getValue() { return value; }
        public void setValue(Double value) { this.value = value; }
        
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }
    
    // Main getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public LocalDateTime getTestTimestamp() { return testTimestamp; }
    public void setTestTimestamp(LocalDateTime testTimestamp) { this.testTimestamp = testTimestamp; }
    
    public ClientInfo getClientInfo() { return clientInfo; }
    public void setClientInfo(ClientInfo clientInfo) { this.clientInfo = clientInfo; }
    
    public ServerInfo getServerInfo() { return serverInfo; }
    public void setServerInfo(ServerInfo serverInfo) { this.serverInfo = serverInfo; }
    
    public SpeedMetrics getDownloadMetrics() { return downloadMetrics; }
    public void setDownloadMetrics(SpeedMetrics downloadMetrics) { this.downloadMetrics = downloadMetrics; }
    
    public SpeedMetrics getUploadMetrics() { return uploadMetrics; }
    public void setUploadMetrics(SpeedMetrics uploadMetrics) { this.uploadMetrics = uploadMetrics; }
    
    public LatencyMetrics getLatencyMetrics() { return latencyMetrics; }
    public void setLatencyMetrics(LatencyMetrics latencyMetrics) { this.latencyMetrics = latencyMetrics; }
    
    public TestConfiguration getTestConfiguration() { return testConfiguration; }
    public void setTestConfiguration(TestConfiguration testConfiguration) { this.testConfiguration = testConfiguration; }
    
    public StatisticalSummary getStatisticalSummary() { return statisticalSummary; }
    public void setStatisticalSummary(StatisticalSummary statisticalSummary) { this.statisticalSummary = statisticalSummary; }
    
    public List<RawMeasurement> getRawMeasurements() { return rawMeasurements; }
    public void setRawMeasurements(List<RawMeasurement> rawMeasurements) { this.rawMeasurements = rawMeasurements; }
}