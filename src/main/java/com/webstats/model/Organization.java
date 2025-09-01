package com.webstats.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Document(collection = "organizations")
public class Organization {
    
    @Id
    private String id;
    
    @NotBlank(message = "Organization name is required")
    @Field("name")
    private String name;
    
    @Field("description")
    private String description;
    
    @Email(message = "Invalid email format")
    @Field("contact_email")
    private String contactEmail;
    
    @Field("subscription_plan")
    private SubscriptionPlan subscriptionPlan;
    
    @Field("created_at")
    private LocalDateTime createdAt;
    
    @Field("updated_at")
    private LocalDateTime updatedAt;
    
    @Field("is_active")
    private Boolean isActive;
    
    @Field("test_limits")
    private TestLimits testLimits;
    
    @Field("users")
    private List<String> userIds;
    
    public Organization() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isActive = true;
        this.userIds = new ArrayList<>();
        this.subscriptionPlan = SubscriptionPlan.FREE;
        this.testLimits = new TestLimits();
    }
    
    public enum SubscriptionPlan {
        FREE, BASIC, PREMIUM, ENTERPRISE
    }
    
    public static class TestLimits {
        @Field("daily_tests")
        private Integer dailyTests = 100;
        
        @Field("concurrent_tests")
        private Integer concurrentTests = 5;
        
        @Field("data_retention_days")
        private Integer dataRetentionDays = 30;
        
        public TestLimits() {}
        
        public Integer getDailyTests() { return dailyTests; }
        public void setDailyTests(Integer dailyTests) { this.dailyTests = dailyTests; }
        
        public Integer getConcurrentTests() { return concurrentTests; }
        public void setConcurrentTests(Integer concurrentTests) { this.concurrentTests = concurrentTests; }
        
        public Integer getDataRetentionDays() { return dataRetentionDays; }
        public void setDataRetentionDays(Integer dataRetentionDays) { this.dataRetentionDays = dataRetentionDays; }
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { 
        this.name = name; 
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { 
        this.description = description; 
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { 
        this.contactEmail = contactEmail; 
        this.updatedAt = LocalDateTime.now();
    }
    
    public SubscriptionPlan getSubscriptionPlan() { return subscriptionPlan; }
    public void setSubscriptionPlan(SubscriptionPlan subscriptionPlan) { 
        this.subscriptionPlan = subscriptionPlan; 
        this.updatedAt = LocalDateTime.now();
    }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { 
        this.isActive = isActive; 
        this.updatedAt = LocalDateTime.now();
    }
    
    public TestLimits getTestLimits() { return testLimits; }
    public void setTestLimits(TestLimits testLimits) { 
        this.testLimits = testLimits; 
        this.updatedAt = LocalDateTime.now();
    }
    
    public List<String> getUserIds() { return userIds; }
    public void setUserIds(List<String> userIds) { 
        this.userIds = userIds; 
        this.updatedAt = LocalDateTime.now();
    }
}