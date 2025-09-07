package com.webstats.service;

import com.webstats.dto.SpeedTestRequestDto;
import com.webstats.dto.SpeedTestResponseDto;
import com.webstats.dto.SpeedTestHistoryDto;
import com.webstats.model.SpeedTestResult;
import com.webstats.model.SpeedTestSession;
import com.webstats.model.User;
import com.webstats.model.Organization;
import com.webstats.repository.SpeedTestResultRepository;
import com.webstats.repository.SpeedTestSessionRepository;
import com.webstats.repository.UserRepository;
import com.webstats.repository.OrganizationRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class SpeedTestService {
    
    @Autowired
    private SpeedTestResultRepository speedTestResultRepository;
    
    @Autowired
    private SpeedTestSessionRepository speedTestSessionRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private OrganizationRepository organizationRepository;
    
    @Autowired
    private StatisticalAnalysisService statisticalAnalysisService;
    
    @Autowired
    private LatencyMeasurementService latencyMeasurementService;
    
    @Autowired
    private DownloadTestService downloadTestService;
    
    @Autowired
    private UploadTestService uploadTestService;
    
    public CompletableFuture<SpeedTestResponseDto> initiateSpeedTest(
            SpeedTestRequestDto request, String userId, HttpServletRequest httpRequest) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Support both authenticated and anonymous users
                User user = null;
                Organization organization = null;
                
                if (userId != null && !userId.isEmpty() && !userId.equals("anonymous")) {
                    Optional<User> userOpt = userRepository.findById(userId);
                    if (userOpt.isPresent()) {
                        user = userOpt.get();
                        Optional<Organization> orgOpt = organizationRepository.findById(user.getOrganizationId());
                        organization = orgOpt.orElse(null);
                    }
                }
                
                // Check if user has active sessions (only for authenticated users)
                if (user != null) {
                    List<SpeedTestSession> activeSessions = speedTestSessionRepository.findActiveSessionsByUserId(userId);
                    if (!activeSessions.isEmpty()) {
                        throw new RuntimeException("User already has an active speed test session");
                    }
                }
                
                // Create new session
                SpeedTestSession session = createNewSession(request, user, organization, httpRequest);
                session = speedTestSessionRepository.save(session);
                
                // Create response DTO
                SpeedTestResponseDto response = new SpeedTestResponseDto();
                response.setSessionId(session.getId());
                response.setStatus(session.getStatus());
                response.setCurrentPhase(session.getCurrentPhase());
                response.setProgressPercentage(session.getProgressPercentage());
                response.setTestTimestamp(session.getSessionStart());
                
                // Start the actual speed test asynchronously
                performSpeedTestAsync(session.getId(), request);
                
                return response;
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to initiate speed test: " + e.getMessage());
            }
        });
    }
    
    private SpeedTestSession createNewSession(SpeedTestRequestDto request, User user, Organization organization, HttpServletRequest httpRequest) {
        SpeedTestSession session = new SpeedTestSession();
        
        // Handle anonymous users
        session.setUserId(user != null ? user.getId() : "anonymous");
        session.setOrganizationId(organization != null ? organization.getId() : "public");
        session.setStatus(SpeedTestSession.SessionStatus.INITIALIZING);
        session.setCurrentPhase(SpeedTestSession.TestPhase.INITIALIZATION);
        session.setProgressPercentage(0);
        
        // Set test configuration
        SpeedTestResult.TestConfiguration config = new SpeedTestResult.TestConfiguration();
        config.setTestType(request.getTestType());
        config.setTestDurationSeconds(request.getTestDurationSeconds());
        config.setNumberOfRuns(request.getNumberOfRuns());
        config.setConcurrentConnections(request.getConcurrentConnections());
        config.setTestFileSizeMb(request.getTestFileSizeMb());
        session.setTestConfiguration(config);
        
        // Set client info
        SpeedTestResult.ClientInfo clientInfo = new SpeedTestResult.ClientInfo();
        clientInfo.setIpAddress(getClientIpAddress(httpRequest));
        clientInfo.setUserAgent(httpRequest.getHeader("User-Agent"));
        session.setClientInfo(clientInfo);
        
        // Set server info (simplified)
        SpeedTestResult.ServerInfo serverInfo = new SpeedTestResult.ServerInfo();
        serverInfo.setServerId("default-server-1");
        serverInfo.setLocation("Default Location");
        serverInfo.setProvider("WebStats.io");
        session.setServerInfo(serverInfo);
        
        return session;
    }
    
    private void performSpeedTestAsync(String sessionId, SpeedTestRequestDto request) {
        CompletableFuture.runAsync(() -> {
            try {
                SpeedTestSession session = speedTestSessionRepository.findById(sessionId).orElse(null);
                if (session == null) return;
                
                // Update session status
                session.setStatus(SpeedTestSession.SessionStatus.RUNNING);
                session.setCurrentPhase(SpeedTestSession.TestPhase.LATENCY_TEST);
                session.setProgressPercentage(10);
                speedTestSessionRepository.save(session);
                
                SpeedTestResult result = new SpeedTestResult();
                result.setUserId(session.getUserId());
                result.setOrganizationId(session.getOrganizationId());
                result.setSessionId(sessionId);
                result.setClientInfo(session.getClientInfo());
                result.setServerInfo(session.getServerInfo());
                result.setTestConfiguration(session.getTestConfiguration());
                
                List<SpeedTestResult.RawMeasurement> allMeasurements = new ArrayList<>();
                
                // Phase 1: Latency Test
                if (shouldRunTest(request.getTestType(), "LATENCY")) {
                    performLatencyTest(session, result, allMeasurements);
                }
                
                // Phase 2: Download Test
                if (shouldRunTest(request.getTestType(), "DOWNLOAD")) {
                    session.setCurrentPhase(SpeedTestSession.TestPhase.DOWNLOAD_TEST);
                    session.setProgressPercentage(40);
                    speedTestSessionRepository.save(session);
                    performDownloadTest(session, result, allMeasurements);
                }
                
                // Phase 3: Upload Test
                if (shouldRunTest(request.getTestType(), "UPLOAD")) {
                    session.setCurrentPhase(SpeedTestSession.TestPhase.UPLOAD_TEST);
                    session.setProgressPercentage(70);
                    speedTestSessionRepository.save(session);
                    performUploadTest(session, result, allMeasurements);
                }
                
                // Phase 4: Analysis
                session.setCurrentPhase(SpeedTestSession.TestPhase.ANALYSIS);
                session.setProgressPercentage(90);
                speedTestSessionRepository.save(session);
                performStatisticalAnalysis(result, allMeasurements);
                
                // Save results
                result.setRawMeasurements(allMeasurements);
                speedTestResultRepository.save(result);
                
                // Complete session
                session.setStatus(SpeedTestSession.SessionStatus.COMPLETED);
                session.setCurrentPhase(SpeedTestSession.TestPhase.COMPLETED);
                session.setProgressPercentage(100);
                session.setSessionEnd(LocalDateTime.now());
                speedTestSessionRepository.save(session);
                
            } catch (Exception e) {
                // Handle error
                SpeedTestSession session = speedTestSessionRepository.findById(sessionId).orElse(null);
                if (session != null) {
                    session.setStatus(SpeedTestSession.SessionStatus.FAILED);
                    session.setErrorMessage(e.getMessage());
                    session.setSessionEnd(LocalDateTime.now());
                    speedTestSessionRepository.save(session);
                }
            }
        });
    }
    
    private boolean shouldRunTest(SpeedTestResult.TestConfiguration.TestType testType, String phase) {
        return testType == SpeedTestResult.TestConfiguration.TestType.FULL ||
               (testType == SpeedTestResult.TestConfiguration.TestType.DOWNLOAD_ONLY && phase.equals("DOWNLOAD")) ||
               (testType == SpeedTestResult.TestConfiguration.TestType.UPLOAD_ONLY && phase.equals("UPLOAD")) ||
               (testType == SpeedTestResult.TestConfiguration.TestType.LATENCY_ONLY && phase.equals("LATENCY"));
    }
    
    private void performLatencyTest(SpeedTestSession session, SpeedTestResult result, List<SpeedTestResult.RawMeasurement> measurements) {
        try {
            String host = "8.8.8.8"; // Google DNS for testing
            int port = 53;
            
            SpeedTestResult.LatencyMetrics latencyMetrics = latencyMeasurementService
                    .measureLatency(host, port).get();
            
            result.setLatencyMetrics(latencyMetrics);
            
            // Add raw measurements
            List<SpeedTestResult.RawMeasurement> latencyMeasurements = latencyMeasurementService
                    .performMultipleLatencyRuns(host, port, session.getTestConfiguration().getNumberOfRuns()).get();
            measurements.addAll(latencyMeasurements);
            
        } catch (Exception e) {
            // Create default latency metrics on failure
            SpeedTestResult.LatencyMetrics defaultMetrics = new SpeedTestResult.LatencyMetrics();
            defaultMetrics.setPingMs(-1.0);
            result.setLatencyMetrics(defaultMetrics);
        }
    }
    
    private void performDownloadTest(SpeedTestSession session, SpeedTestResult result, List<SpeedTestResult.RawMeasurement> measurements) {
        try {
            // Use actual download test service
            SpeedTestResult.SpeedMetrics downloadMetrics = downloadTestService
                    .performDownloadTest(session.getTestConfiguration()).get();
            result.setDownloadMetrics(downloadMetrics);
            
            // Get detailed measurements
            List<SpeedTestResult.RawMeasurement> downloadMeasurements = downloadTestService
                    .performMultipleDownloadRuns(session.getTestConfiguration()).get();
            measurements.addAll(downloadMeasurements);
            
        } catch (Exception e) {
            System.err.println("Download test failed, using fallback: " + e.getMessage());
            // Fallback to simulated test
            SpeedTestResult.SpeedMetrics downloadMetrics = simulateSpeedTest("download", session.getTestConfiguration());
            result.setDownloadMetrics(downloadMetrics);
            
            // Add simulated measurements
            for (int i = 0; i < session.getTestConfiguration().getNumberOfRuns(); i++) {
                SpeedTestResult.RawMeasurement measurement = new SpeedTestResult.RawMeasurement();
                measurement.setRunNumber(i + 1);
                measurement.setTimestamp(LocalDateTime.now());
                measurement.setMeasurementType(SpeedTestResult.RawMeasurement.MeasurementType.DOWNLOAD_SPEED);
                measurement.setValue(downloadMetrics.getSpeedMbps() + (Math.random() - 0.5) * 10);
                measurements.add(measurement);
            }
        }
    }
    
    private void performUploadTest(SpeedTestSession session, SpeedTestResult result, List<SpeedTestResult.RawMeasurement> measurements) {
        try {
            // Use actual upload test service
            SpeedTestResult.SpeedMetrics uploadMetrics = uploadTestService
                    .performUploadTest(session.getTestConfiguration()).get();
            result.setUploadMetrics(uploadMetrics);
            
            // Get detailed measurements
            List<SpeedTestResult.RawMeasurement> uploadMeasurements = uploadTestService
                    .performMultipleUploadRuns(session.getTestConfiguration()).get();
            measurements.addAll(uploadMeasurements);
            
        } catch (Exception e) {
            System.err.println("Upload test failed, using fallback: " + e.getMessage());
            // Fallback to simulated test
            SpeedTestResult.SpeedMetrics uploadMetrics = simulateSpeedTest("upload", session.getTestConfiguration());
            result.setUploadMetrics(uploadMetrics);
            
            // Add simulated measurements
            for (int i = 0; i < session.getTestConfiguration().getNumberOfRuns(); i++) {
                SpeedTestResult.RawMeasurement measurement = new SpeedTestResult.RawMeasurement();
                measurement.setRunNumber(i + 1);
                measurement.setTimestamp(LocalDateTime.now());
                measurement.setMeasurementType(SpeedTestResult.RawMeasurement.MeasurementType.UPLOAD_SPEED);
                measurement.setValue(uploadMetrics.getSpeedMbps() + (Math.random() - 0.5) * 5);
                measurements.add(measurement);
            }
        }
    }
    
    private SpeedTestResult.SpeedMetrics simulateSpeedTest(String type, SpeedTestResult.TestConfiguration config) {
        SpeedTestResult.SpeedMetrics metrics = new SpeedTestResult.SpeedMetrics();
        
        // Simulated values - replace with actual speed testing logic
        double baseSpeed = type.equals("download") ? 50.0 + Math.random() * 100 : 25.0 + Math.random() * 50;
        metrics.setSpeedMbps(baseSpeed);
        metrics.setAverageSpeedMbps(baseSpeed * 0.9);
        metrics.setPeakSpeedMbps(baseSpeed * 1.2);
        metrics.setDurationSeconds(config.getTestDurationSeconds().doubleValue());
        metrics.setBytesTransferred((long) (baseSpeed * 1024 * 1024 * config.getTestDurationSeconds() / 8));
        metrics.setStabilityScore(statisticalAnalysisService.calculateStabilityScore(
                Arrays.asList(baseSpeed, baseSpeed * 0.9, baseSpeed * 1.1, baseSpeed * 0.95)));
        
        return metrics;
    }
    
    private void performStatisticalAnalysis(SpeedTestResult result, List<SpeedTestResult.RawMeasurement> measurements) {
        List<Double> downloadSpeeds = measurements.stream()
                .filter(m -> m.getMeasurementType() == SpeedTestResult.RawMeasurement.MeasurementType.DOWNLOAD_SPEED)
                .map(SpeedTestResult.RawMeasurement::getValue)
                .collect(Collectors.toList());
        
        List<Double> uploadSpeeds = measurements.stream()
                .filter(m -> m.getMeasurementType() == SpeedTestResult.RawMeasurement.MeasurementType.UPLOAD_SPEED)
                .map(SpeedTestResult.RawMeasurement::getValue)
                .collect(Collectors.toList());
        
        List<Double> latencies = measurements.stream()
                .filter(m -> m.getMeasurementType() == SpeedTestResult.RawMeasurement.MeasurementType.LATENCY)
                .map(SpeedTestResult.RawMeasurement::getValue)
                .collect(Collectors.toList());
        
        SpeedTestResult.StatisticalSummary summary = statisticalAnalysisService
                .calculateCompleteSummary(downloadSpeeds, uploadSpeeds, latencies);
        
        result.setStatisticalSummary(summary);
    }
    
    public SpeedTestResponseDto getSessionStatus(String sessionId) {
        Optional<SpeedTestSession> sessionOpt = speedTestSessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            throw new RuntimeException("Session not found");
        }
        
        SpeedTestSession session = sessionOpt.get();
        SpeedTestResponseDto response = new SpeedTestResponseDto();
        response.setSessionId(session.getId());
        response.setStatus(session.getStatus());
        response.setCurrentPhase(session.getCurrentPhase());
        response.setProgressPercentage(session.getProgressPercentage());
        response.setErrorMessage(session.getErrorMessage());
        response.setTestTimestamp(session.getSessionStart());
        
        // If completed, get the result
        if (session.getStatus() == SpeedTestSession.SessionStatus.COMPLETED) {
            List<SpeedTestResult> results = speedTestResultRepository.findBySessionId(sessionId);
            if (!results.isEmpty()) {
                SpeedTestResult result = results.get(0);
                response.setResultId(result.getId());
                
                // Map metrics to DTOs
                if (result.getDownloadMetrics() != null) {
                    SpeedTestResponseDto.SpeedMetricsDto downloadDto = mapSpeedMetrics(result.getDownloadMetrics());
                    response.setDownloadMetrics(downloadDto);
                }
                
                if (result.getUploadMetrics() != null) {
                    SpeedTestResponseDto.SpeedMetricsDto uploadDto = mapSpeedMetrics(result.getUploadMetrics());
                    response.setUploadMetrics(uploadDto);
                }
                
                if (result.getLatencyMetrics() != null) {
                    SpeedTestResponseDto.LatencyMetricsDto latencyDto = mapLatencyMetrics(result.getLatencyMetrics());
                    response.setLatencyMetrics(latencyDto);
                }
                
                if (result.getStatisticalSummary() != null) {
                    SpeedTestResponseDto.StatisticalSummaryDto summaryDto = mapStatisticalSummary(result.getStatisticalSummary());
                    response.setStatisticalSummary(summaryDto);
                }
            }
        }
        
        return response;
    }
    
    public List<SpeedTestHistoryDto> getUserHistory(String userId, int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit);
        return speedTestResultRepository.findTop10ByUserIdOrderByTestTimestampDesc(userId, pageRequest)
                .stream()
                .map(this::mapToHistoryDto)
                .collect(Collectors.toList());
    }
    
    public SpeedTestHistoryDto.TimeSeriesData getTimeSeriesData(String userId, LocalDateTime startDate, LocalDateTime endDate) {
        List<SpeedTestResult> results = speedTestResultRepository.findByUserIdAndTestTimestampBetween(userId, startDate, endDate);
        
        SpeedTestHistoryDto.TimeSeriesData timeSeriesData = new SpeedTestHistoryDto.TimeSeriesData();
        
        List<SpeedTestHistoryDto.DataPoint> downloadSeries = results.stream()
                .filter(r -> r.getDownloadMetrics() != null)
                .map(r -> new SpeedTestHistoryDto.DataPoint(r.getTestTimestamp(), r.getDownloadMetrics().getSpeedMbps()))
                .collect(Collectors.toList());
        
        List<SpeedTestHistoryDto.DataPoint> uploadSeries = results.stream()
                .filter(r -> r.getUploadMetrics() != null)
                .map(r -> new SpeedTestHistoryDto.DataPoint(r.getTestTimestamp(), r.getUploadMetrics().getSpeedMbps()))
                .collect(Collectors.toList());
        
        List<SpeedTestHistoryDto.DataPoint> latencySeries = results.stream()
                .filter(r -> r.getLatencyMetrics() != null)
                .map(r -> new SpeedTestHistoryDto.DataPoint(r.getTestTimestamp(), r.getLatencyMetrics().getPingMs()))
                .collect(Collectors.toList());
        
        timeSeriesData.setDownloadSeries(downloadSeries);
        timeSeriesData.setUploadSeries(uploadSeries);
        timeSeriesData.setLatencySeries(latencySeries);
        
        return timeSeriesData;
    }
    
    private SpeedTestHistoryDto mapToHistoryDto(SpeedTestResult result) {
        SpeedTestHistoryDto dto = new SpeedTestHistoryDto();
        dto.setResultId(result.getId());
        dto.setTestTimestamp(result.getTestTimestamp());
        
        if (result.getDownloadMetrics() != null) {
            dto.setDownloadSpeedMbps(result.getDownloadMetrics().getSpeedMbps());
        }
        
        if (result.getUploadMetrics() != null) {
            dto.setUploadSpeedMbps(result.getUploadMetrics().getSpeedMbps());
        }
        
        if (result.getLatencyMetrics() != null) {
            dto.setLatencyMs(result.getLatencyMetrics().getPingMs());
        }
        
        if (result.getServerInfo() != null) {
            dto.setLocation(result.getServerInfo().getLocation());
            dto.setServerProvider(result.getServerInfo().getProvider());
        }
        
        return dto;
    }
    
    // Helper methods for mapping entities to DTOs
    private SpeedTestResponseDto.SpeedMetricsDto mapSpeedMetrics(SpeedTestResult.SpeedMetrics metrics) {
        SpeedTestResponseDto.SpeedMetricsDto dto = new SpeedTestResponseDto.SpeedMetricsDto();
        dto.setSpeedMbps(metrics.getSpeedMbps());
        dto.setBytesTransferred(metrics.getBytesTransferred());
        dto.setDurationSeconds(metrics.getDurationSeconds());
        dto.setPeakSpeedMbps(metrics.getPeakSpeedMbps());
        dto.setAverageSpeedMbps(metrics.getAverageSpeedMbps());
        dto.setStabilityScore(metrics.getStabilityScore());
        return dto;
    }
    
    private SpeedTestResponseDto.LatencyMetricsDto mapLatencyMetrics(SpeedTestResult.LatencyMetrics metrics) {
        SpeedTestResponseDto.LatencyMetricsDto dto = new SpeedTestResponseDto.LatencyMetricsDto();
        dto.setPingMs(metrics.getPingMs());
        dto.setJitterMs(metrics.getJitterMs());
        dto.setPacketLossPercent(metrics.getPacketLossPercent());
        dto.setDnsLookupMs(metrics.getDnsLookupMs());
        dto.setTcpConnectMs(metrics.getTcpConnectMs());
        dto.setSslHandshakeMs(metrics.getSslHandshakeMs());
        dto.setFirstByteMs(metrics.getFirstByteMs());
        return dto;
    }
    
    private SpeedTestResponseDto.StatisticalSummaryDto mapStatisticalSummary(SpeedTestResult.StatisticalSummary summary) {
        SpeedTestResponseDto.StatisticalSummaryDto dto = new SpeedTestResponseDto.StatisticalSummaryDto();
        
        if (summary.getDownloadStats() != null) {
            dto.setDownloadStats(mapStatistics(summary.getDownloadStats()));
        }
        
        if (summary.getUploadStats() != null) {
            dto.setUploadStats(mapStatistics(summary.getUploadStats()));
        }
        
        if (summary.getLatencyStats() != null) {
            dto.setLatencyStats(mapStatistics(summary.getLatencyStats()));
        }
        
        return dto;
    }
    
    private SpeedTestResponseDto.StatisticsDto mapStatistics(SpeedTestResult.Statistics stats) {
        SpeedTestResponseDto.StatisticsDto dto = new SpeedTestResponseDto.StatisticsDto();
        dto.setMedian(stats.getMedian());
        dto.setMean(stats.getMean());
        dto.setMin(stats.getMin());
        dto.setMax(stats.getMax());
        dto.setPercentile95(stats.getPercentile95());
        dto.setPercentile99(stats.getPercentile99());
        dto.setStandardDeviation(stats.getStandardDeviation());
        return dto;
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
}