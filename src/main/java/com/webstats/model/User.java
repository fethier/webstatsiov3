package com.webstats.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;

import java.time.LocalDateTime;

@Document(collection = "users")
public class User {
    
    @Id
    private String id;
    
    @NotBlank(message = "Username is required")
    @Field("username")
    private String username;
    
    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    @Field("email")
    private String email;
    
    @Field("organization_id")
    private String organizationId;
    
    @Field("role")
    private UserRole role;
    
    @Field("created_at")
    private LocalDateTime createdAt;
    
    @Field("updated_at")
    private LocalDateTime updatedAt;
    
    @Field("last_login")
    private LocalDateTime lastLogin;
    
    @Field("is_active")
    private Boolean isActive;
    
    public User() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isActive = true;
        this.role = UserRole.USER;
    }
    
    public enum UserRole {
        USER, ADMIN, ORGANIZATION_ADMIN
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { 
        this.username = username; 
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { 
        this.email = email; 
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { 
        this.organizationId = organizationId; 
        this.updatedAt = LocalDateTime.now();
    }
    
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { 
        this.role = role; 
        this.updatedAt = LocalDateTime.now();
    }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { 
        this.isActive = isActive; 
        this.updatedAt = LocalDateTime.now();
    }
}