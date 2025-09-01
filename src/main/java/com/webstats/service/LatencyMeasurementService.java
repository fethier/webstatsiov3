package com.webstats.service;

import com.webstats.model.SpeedTestResult;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

@Service
public class LatencyMeasurementService {
    
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private static final int DEFAULT_PING_COUNT = 10;
    private static final int DEFAULT_TIMEOUT_MS = 5000;
    
    public CompletableFuture<SpeedTestResult.LatencyMetrics> measureLatency(String host, int port) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                SpeedTestResult.LatencyMetrics metrics = new SpeedTestResult.LatencyMetrics();
                
                // DNS Lookup time
                long dnsStart = System.nanoTime();
                InetAddress address = InetAddress.getByName(host);
                long dnsEnd = System.nanoTime();
                metrics.setDnsLookupMs((dnsEnd - dnsStart) / 1_000_000.0);
                
                // Multiple ping measurements
                List<Double> pingTimes = new ArrayList<>();
                for (int i = 0; i < DEFAULT_PING_COUNT; i++) {
                    Double pingTime = measureSinglePing(address.getHostAddress(), port);
                    if (pingTime != null) {
                        pingTimes.add(pingTime);
                    }
                    
                    if (i < DEFAULT_PING_COUNT - 1) {
                        try {
                            Thread.sleep(100); // 100ms between pings
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
                
                if (!pingTimes.isEmpty()) {
                    metrics.setPingMs(pingTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
                    metrics.setJitterMs(calculateJitter(pingTimes));
                    
                    // Calculate packet loss
                    double packetLoss = ((double) (DEFAULT_PING_COUNT - pingTimes.size()) / DEFAULT_PING_COUNT) * 100;
                    metrics.setPacketLossPercent(packetLoss);
                }
                
                // Detailed connection timing
                measureDetailedTiming(host, port, metrics);
                
                return metrics;
                
            } catch (Exception e) {
                SpeedTestResult.LatencyMetrics errorMetrics = new SpeedTestResult.LatencyMetrics();
                errorMetrics.setPingMs(-1.0); // Indicate error
                return errorMetrics;
            }
        }, executorService);
    }
    
    private Double measureSinglePing(String host, int port) {
        try (Socket socket = new Socket()) {
            long startTime = System.nanoTime();
            socket.connect(new InetSocketAddress(host, port), DEFAULT_TIMEOUT_MS);
            long endTime = System.nanoTime();
            
            return (endTime - startTime) / 1_000_000.0; // Convert to milliseconds
            
        } catch (IOException e) {
            return null; // Failed ping
        }
    }
    
    private void measureDetailedTiming(String host, int port, SpeedTestResult.LatencyMetrics metrics) {
        try (Socket socket = new Socket()) {
            // TCP Connect time
            long tcpStart = System.nanoTime();
            socket.connect(new InetSocketAddress(host, port), DEFAULT_TIMEOUT_MS);
            long tcpEnd = System.nanoTime();
            metrics.setTcpConnectMs((tcpEnd - tcpStart) / 1_000_000.0);
            
            // SSL Handshake time (if applicable - simplified for this example)
            if (port == 443) {
                // This would require actual SSL implementation
                metrics.setSslHandshakeMs(0.0); // Placeholder
            }
            
            // First byte time (simplified)
            long firstByteStart = System.nanoTime();
            socket.getOutputStream().write("GET / HTTP/1.1\r\nHost: ".concat(host).concat("\r\n\r\n").getBytes());
            socket.getOutputStream().flush();
            
            // Read first byte
            int firstByte = socket.getInputStream().read();
            long firstByteEnd = System.nanoTime();
            
            if (firstByte != -1) {
                metrics.setFirstByteMs((firstByteEnd - firstByteStart) / 1_000_000.0);
            }
            
        } catch (IOException e) {
            // Set default values for failed measurements
            metrics.setTcpConnectMs(-1.0);
            metrics.setSslHandshakeMs(-1.0);
            metrics.setFirstByteMs(-1.0);
        }
    }
    
    private double calculateJitter(List<Double> pingTimes) {
        if (pingTimes.size() < 2) {
            return 0.0;
        }
        
        double sumOfSquaredDifferences = 0.0;
        for (int i = 1; i < pingTimes.size(); i++) {
            double diff = pingTimes.get(i) - pingTimes.get(i - 1);
            sumOfSquaredDifferences += diff * diff;
        }
        
        return Math.sqrt(sumOfSquaredDifferences / (pingTimes.size() - 1));
    }
    
    public CompletableFuture<List<SpeedTestResult.RawMeasurement>> performMultipleLatencyRuns(
            String host, int port, int numberOfRuns) {
        
        return CompletableFuture.supplyAsync(() -> {
            List<SpeedTestResult.RawMeasurement> measurements = new ArrayList<>();
            
            IntStream.range(0, numberOfRuns).forEach(runNumber -> {
                try {
                    Double pingTime = measureSinglePing(host, port);
                    if (pingTime != null) {
                        SpeedTestResult.RawMeasurement measurement = new SpeedTestResult.RawMeasurement();
                        measurement.setRunNumber(runNumber + 1);
                        measurement.setTimestamp(java.time.LocalDateTime.now());
                        measurement.setMeasurementType(SpeedTestResult.RawMeasurement.MeasurementType.LATENCY);
                        measurement.setValue(pingTime);
                        measurements.add(measurement);
                    }
                    
                    if (runNumber < numberOfRuns - 1) {
                        try {
                            Thread.sleep(500); // 500ms between runs
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                } catch (Exception e) {
                    // Skip failed measurement
                }
            });
            
            return measurements;
        }, executorService);
    }
    
    public void shutdown() {
        executorService.shutdown();
    }
}