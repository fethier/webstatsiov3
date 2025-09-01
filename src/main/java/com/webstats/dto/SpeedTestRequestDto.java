package com.webstats.dto;

import com.webstats.model.SpeedTestResult;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;

public class SpeedTestRequestDto {
    
    @NotNull(message = "Test type is required")
    private SpeedTestResult.TestConfiguration.TestType testType;
    
    @Min(value = 5, message = "Test duration must be at least 5 seconds")
    @Max(value = 60, message = "Test duration cannot exceed 60 seconds")
    private Integer testDurationSeconds = 10;
    
    @Min(value = 1, message = "Number of runs must be at least 1")
    @Max(value = 10, message = "Number of runs cannot exceed 10")
    private Integer numberOfRuns = 3;
    
//    @Min(value = 1, message = "Concurrent connections must be at least 1")
//    @Max(value = 8, message = "Concurrent connections cannot exceed 8")
    private Integer concurrentConnections = 4;
    
    private Double testFileSizeMb;
    
    private String preferredServerId;
    
    public SpeedTestRequestDto() {}
    
    public SpeedTestResult.TestConfiguration.TestType getTestType() { return testType; }
    public void setTestType(SpeedTestResult.TestConfiguration.TestType testType) { this.testType = testType; }
    
    public Integer getTestDurationSeconds() { return testDurationSeconds; }
    public void setTestDurationSeconds(Integer testDurationSeconds) { this.testDurationSeconds = testDurationSeconds; }
    
    public Integer getNumberOfRuns() { return numberOfRuns; }
    public void setNumberOfRuns(Integer numberOfRuns) { this.numberOfRuns = numberOfRuns; }
    
    public Integer getConcurrentConnections() { return concurrentConnections; }
    public void setConcurrentConnections(Integer concurrentConnections) { this.concurrentConnections = concurrentConnections; }
    
    public Double getTestFileSizeMb() { return testFileSizeMb; }
    public void setTestFileSizeMb(Double testFileSizeMb) { this.testFileSizeMb = testFileSizeMb; }
    
    public String getPreferredServerId() { return preferredServerId; }
    public void setPreferredServerId(String preferredServerId) { this.preferredServerId = preferredServerId; }
    
    @Override
    public String toString() {
        return "SpeedTestRequestDto{" +
                "testType=" + testType +
                ", testDurationSeconds=" + testDurationSeconds +
                ", numberOfRuns=" + numberOfRuns +
                ", concurrentConnections=" + concurrentConnections +
                ", testFileSizeMb=" + testFileSizeMb +
                ", preferredServerId='" + preferredServerId + '\'' +
                '}';
    }
}