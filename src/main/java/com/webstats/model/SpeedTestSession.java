package com.webstats.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Document(collection = "speed_test_sessions")
public class SpeedTestSession {
    
    @Id
    private String id;
    
    @Field("user_id")
    private String userId;
    
    @Field("organization_id")
    private String organizationId;
    
    @NotNull
    @Field("session_start")
    private LocalDateTime sessionStart;
    
    @Field("session_end")
    private LocalDateTime sessionEnd;
    
    @Field("status")
    private SessionStatus status;
    
    @Field("test_configuration")
    private SpeedTestResult.TestConfiguration testConfiguration;
    
    @Field("current_phase")
    private TestPhase currentPhase;
    
    @Field("progress_percentage")
    private Integer progressPercentage;
    
    @Field("error_message")
    private String errorMessage;
    
    @Field("client_info")
    private SpeedTestResult.ClientInfo clientInfo;
    
    @Field("server_info")
    private SpeedTestResult.ServerInfo serverInfo;
    
    public SpeedTestSession() {
        this.sessionStart = LocalDateTime.now();
        this.status = SessionStatus.INITIALIZING;
        this.currentPhase = TestPhase.INITIALIZATION;
        this.progressPercentage = 0;
    }
    
    public enum SessionStatus {
        INITIALIZING, RUNNING, COMPLETED, FAILED, CANCELLED
    }
    
    public enum TestPhase {
        INITIALIZATION, LATENCY_TEST, DOWNLOAD_TEST, UPLOAD_TEST, ANALYSIS, COMPLETED
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
    
    public LocalDateTime getSessionStart() { return sessionStart; }
    public void setSessionStart(LocalDateTime sessionStart) { this.sessionStart = sessionStart; }
    
    public LocalDateTime getSessionEnd() { return sessionEnd; }
    public void setSessionEnd(LocalDateTime sessionEnd) { this.sessionEnd = sessionEnd; }
    
    public SessionStatus getStatus() { return status; }
    public void setStatus(SessionStatus status) { this.status = status; }
    
    public SpeedTestResult.TestConfiguration getTestConfiguration() { return testConfiguration; }
    public void setTestConfiguration(SpeedTestResult.TestConfiguration testConfiguration) { this.testConfiguration = testConfiguration; }
    
    public TestPhase getCurrentPhase() { return currentPhase; }
    public void setCurrentPhase(TestPhase currentPhase) { this.currentPhase = currentPhase; }
    
    public Integer getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(Integer progressPercentage) { this.progressPercentage = progressPercentage; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public SpeedTestResult.ClientInfo getClientInfo() { return clientInfo; }
    public void setClientInfo(SpeedTestResult.ClientInfo clientInfo) { this.clientInfo = clientInfo; }
    
    public SpeedTestResult.ServerInfo getServerInfo() { return serverInfo; }
    public void setServerInfo(SpeedTestResult.ServerInfo serverInfo) { this.serverInfo = serverInfo; }
}