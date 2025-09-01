package com.webstats.repository;

import com.webstats.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    List<User> findByOrganizationId(String organizationId);
    
    List<User> findByRole(User.UserRole role);
    
    List<User> findByIsActiveTrue();
    
    List<User> findByOrganizationIdAndIsActiveTrue(String organizationId);
    
    @Query("{ 'lastLogin' : { $gte : ?0 } }")
    List<User> findByLastLoginAfter(LocalDateTime dateTime);
    
    @Query("{ 'organizationId' : ?0, 'role' : ?1, 'isActive' : true }")
    List<User> findActiveUsersByOrganizationAndRole(String organizationId, User.UserRole role);
}