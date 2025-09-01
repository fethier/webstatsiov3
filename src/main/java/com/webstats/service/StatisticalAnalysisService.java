package com.webstats.service;

import com.webstats.model.SpeedTestResult;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.DoubleStream;

@Service
public class StatisticalAnalysisService {
    
    public SpeedTestResult.Statistics calculateStatistics(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return new SpeedTestResult.Statistics();
        }
        
        DescriptiveStatistics stats = new DescriptiveStatistics();
        values.forEach(stats::addValue);
        
        SpeedTestResult.Statistics result = new SpeedTestResult.Statistics();
        result.setMean(stats.getMean());
        result.setMedian(stats.getPercentile(50));
        result.setMin(stats.getMin());
        result.setMax(stats.getMax());
        result.setPercentile95(stats.getPercentile(95));
        result.setPercentile99(stats.getPercentile(99));
        result.setStandardDeviation(stats.getStandardDeviation());
        
        return result;
    }
    
    public SpeedTestResult.Statistics calculateStatistics(double[] values) {
        if (values == null || values.length == 0) {
            return new SpeedTestResult.Statistics();
        }
        
        DescriptiveStatistics stats = new DescriptiveStatistics();
        DoubleStream.of(values).forEach(stats::addValue);
        
        SpeedTestResult.Statistics result = new SpeedTestResult.Statistics();
        result.setMean(stats.getMean());
        result.setMedian(stats.getPercentile(50));
        result.setMin(stats.getMin());
        result.setMax(stats.getMax());
        result.setPercentile95(stats.getPercentile(95));
        result.setPercentile99(stats.getPercentile(99));
        result.setStandardDeviation(stats.getStandardDeviation());
        
        return result;
    }
    
    public SpeedTestResult.StatisticalSummary calculateCompleteSummary(
            List<Double> downloadSpeeds, 
            List<Double> uploadSpeeds, 
            List<Double> latencies) {
        
        SpeedTestResult.StatisticalSummary summary = new SpeedTestResult.StatisticalSummary();
        
        if (downloadSpeeds != null && !downloadSpeeds.isEmpty()) {
            summary.setDownloadStats(calculateStatistics(downloadSpeeds));
        }
        
        if (uploadSpeeds != null && !uploadSpeeds.isEmpty()) {
            summary.setUploadStats(calculateStatistics(uploadSpeeds));
        }
        
        if (latencies != null && !latencies.isEmpty()) {
            summary.setLatencyStats(calculateStatistics(latencies));
        }
        
        return summary;
    }
    
    public double calculateStabilityScore(List<Double> values) {
        if (values == null || values.size() < 2) {
            return 100.0;
        }
        
        DescriptiveStatistics stats = new DescriptiveStatistics();
        values.forEach(stats::addValue);
        
        double mean = stats.getMean();
        double stdDev = stats.getStandardDeviation();
        
        if (mean == 0) {
            return 100.0;
        }
        
        double coefficientOfVariation = (stdDev / mean) * 100;
        return Math.max(0, 100 - coefficientOfVariation);
    }
    
    public double calculateJitter(List<Double> latencies) {
        if (latencies == null || latencies.size() < 2) {
            return 0.0;
        }
        
        double sumOfSquaredDifferences = 0.0;
        for (int i = 1; i < latencies.size(); i++) {
            double diff = latencies.get(i) - latencies.get(i - 1);
            sumOfSquaredDifferences += diff * diff;
        }
        
        return Math.sqrt(sumOfSquaredDifferences / (latencies.size() - 1));
    }
    
    public boolean isOutlier(double value, List<Double> dataset) {
        if (dataset == null || dataset.size() < 4) {
            return false;
        }
        
        DescriptiveStatistics stats = new DescriptiveStatistics();
        dataset.forEach(stats::addValue);
        
        double q1 = stats.getPercentile(25);
        double q3 = stats.getPercentile(75);
        double iqr = q3 - q1;
        
        double lowerBound = q1 - 1.5 * iqr;
        double upperBound = q3 + 1.5 * iqr;
        
        return value < lowerBound || value > upperBound;
    }
    
    public List<Double> removeOutliers(List<Double> values) {
        if (values == null || values.size() < 4) {
            return values;
        }
        
        return values.stream()
                .filter(value -> !isOutlier(value, values))
                .toList();
    }
    
    public double calculateConfidenceInterval(List<Double> values, double confidenceLevel) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }
        
        DescriptiveStatistics stats = new DescriptiveStatistics();
        values.forEach(stats::addValue);
        
        double mean = stats.getMean();
        double stdDev = stats.getStandardDeviation();
        double n = values.size();
        
        double tScore = getTScore(confidenceLevel, n - 1);
        return tScore * (stdDev / Math.sqrt(n));
    }
    
    private double getTScore(double confidenceLevel, double degreesOfFreedom) {
        if (confidenceLevel == 0.95 && degreesOfFreedom >= 30) {
            return 1.96;
        } else if (confidenceLevel == 0.99 && degreesOfFreedom >= 30) {
            return 2.58;
        } else if (confidenceLevel == 0.90 && degreesOfFreedom >= 30) {
            return 1.645;
        }
        return 2.0;
    }
}