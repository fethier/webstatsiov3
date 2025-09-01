package com.webstats.repository;

import com.webstats.model.SpeedTestResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SpeedTestResultRepository extends MongoRepository<SpeedTestResult, String> {
    
    List<SpeedTestResult> findByUserId(String userId);
    
    List<SpeedTestResult> findByOrganizationId(String organizationId);
    
    Page<SpeedTestResult> findByUserIdOrderByTestTimestampDesc(String userId, Pageable pageable);
    
    Page<SpeedTestResult> findByOrganizationIdOrderByTestTimestampDesc(String organizationId, Pageable pageable);
    
    @Query("{ 'testTimestamp' : { $gte : ?0, $lte : ?1 } }")
    List<SpeedTestResult> findByTestTimestampBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("{ 'userId' : ?0, 'testTimestamp' : { $gte : ?1, $lte : ?2 } }")
    List<SpeedTestResult> findByUserIdAndTestTimestampBetween(String userId, LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("{ 'organizationId' : ?0, 'testTimestamp' : { $gte : ?1, $lte : ?2 } }")
    List<SpeedTestResult> findByOrganizationIdAndTestTimestampBetween(String organizationId, LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("{ 'sessionId' : ?0 }")
    List<SpeedTestResult> findBySessionId(String sessionId);
    
    @Query("{ 'clientInfo.geolocation.country' : ?0 }")
    List<SpeedTestResult> findByCountry(String country);
    
    @Query("{ 'clientInfo.geolocation.city' : ?0 }")
    List<SpeedTestResult> findByCity(String city);
    
    @Query("{ 'serverInfo.location' : ?0 }")
    List<SpeedTestResult> findByServerLocation(String location);
    
    @Aggregation(pipeline = {
        "{ $match: { 'userId': ?0, 'testTimestamp': { $gte: ?1, $lte: ?2 } } }",
        "{ $group: { _id: null, avgDownload: { $avg: '$downloadMetrics.speedMbps' }, avgUpload: { $avg: '$uploadMetrics.speedMbps' }, avgLatency: { $avg: '$latencyMetrics.pingMs' } } }"
    })
    Object getAverageMetricsByUserAndDateRange(String userId, LocalDateTime startDate, LocalDateTime endDate);
    
    @Aggregation(pipeline = {
        "{ $match: { 'organizationId': ?0, 'testTimestamp': { $gte: ?1, $lte: ?2 } } }",
        "{ $group: { _id: { $dateToString: { format: '%Y-%m-%d', date: '$testTimestamp' } }, count: { $sum: 1 }, avgDownload: { $avg: '$downloadMetrics.speedMbps' }, avgUpload: { $avg: '$uploadMetrics.speedMbps' }, avgLatency: { $avg: '$latencyMetrics.pingMs' } } }",
        "{ $sort: { '_id': 1 } }"
    })
    List<Object> getDailyStatsByOrganizationAndDateRange(String organizationId, LocalDateTime startDate, LocalDateTime endDate);
    
    @Query(value = "{ 'userId' : ?0 }", sort = "{ 'testTimestamp' : -1 }")
    List<SpeedTestResult> findTop10ByUserIdOrderByTestTimestampDesc(String userId, Pageable pageable);
}