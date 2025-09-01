package com.webstats.repository;

import com.webstats.model.SpeedTestSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpeedTestSessionRepository extends MongoRepository<SpeedTestSession, String> {
    
    List<SpeedTestSession> findByUserId(String userId);
    
    List<SpeedTestSession> findByOrganizationId(String organizationId);
    
    List<SpeedTestSession> findByStatus(SpeedTestSession.SessionStatus status);
    
    @Query("{ 'userId' : ?0, 'status' : ?1 }")
    List<SpeedTestSession> findByUserIdAndStatus(String userId, SpeedTestSession.SessionStatus status);
    
    @Query("{ 'userId' : ?0, 'status' : { $in : ['INITIALIZING', 'RUNNING'] } }")
    List<SpeedTestSession> findActiveSessionsByUserId(String userId);
    
    @Query("{ 'organizationId' : ?0, 'status' : { $in : ['INITIALIZING', 'RUNNING'] } }")
    List<SpeedTestSession> findActiveSessionsByOrganizationId(String organizationId);
    
    @Query("{ 'sessionStart' : { $lt : ?0 }, 'status' : { $in : ['INITIALIZING', 'RUNNING'] } }")
    List<SpeedTestSession> findStaleActiveSessions(LocalDateTime cutoffTime);
    
    @Query(value = "{ 'userId' : ?0 }", sort = "{ 'sessionStart' : -1 }")
    Optional<SpeedTestSession> findLatestByUserId(String userId);
    
    @Query("{ 'sessionStart' : { $gte : ?0, $lte : ?1 } }")
    List<SpeedTestSession> findBySessionStartBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("{ 'status' : 'COMPLETED', 'sessionEnd' : { $gte : ?0, $lte : ?1 } }")
    List<SpeedTestSession> findCompletedSessionsBetween(LocalDateTime startDate, LocalDateTime endDate);
}