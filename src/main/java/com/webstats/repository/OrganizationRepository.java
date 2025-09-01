package com.webstats.repository;

import com.webstats.model.Organization;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationRepository extends MongoRepository<Organization, String> {
    
    Optional<Organization> findByName(String name);
    
    Optional<Organization> findByContactEmail(String contactEmail);
    
    List<Organization> findByIsActiveTrue();
    
    List<Organization> findBySubscriptionPlan(Organization.SubscriptionPlan subscriptionPlan);
    
    @Query("{ 'users' : ?0 }")
    Optional<Organization> findByUserId(String userId);
    
    @Query("{ 'isActive' : true, 'subscriptionPlan' : { $in : ?0 } }")
    List<Organization> findActiveBySubscriptionPlans(List<Organization.SubscriptionPlan> plans);
}