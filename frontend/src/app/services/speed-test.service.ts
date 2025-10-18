import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, interval, switchMap, takeWhile } from 'rxjs';

export interface SpeedTestRequest {
  testType: 'FULL' | 'DOWNLOAD_ONLY' | 'UPLOAD_ONLY' | 'LATENCY_ONLY';
  testDurationSeconds: number;
  numberOfRuns: number;
  concurrentConnections: number;
  testFileSizeMb?: number;
  preferredServerId?: string;
}

export interface SpeedTestResponse {
  sessionId: string;
  resultId?: string;
  testTimestamp: string;
  status: 'INITIALIZING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
  currentPhase: 'INITIALIZATION' | 'LATENCY_TEST' | 'DOWNLOAD_TEST' | 'UPLOAD_TEST' | 'ANALYSIS' | 'COMPLETED';
  progressPercentage: number;
  errorMessage?: string;
  downloadMetrics?: SpeedMetrics;
  uploadMetrics?: SpeedMetrics;
  latencyMetrics?: LatencyMetrics;
  statisticalSummary?: StatisticalSummary;
}

export interface SpeedMetrics {
  speedMbps: number;
  bytesTransferred: number;
  durationSeconds: number;
  peakSpeedMbps: number;
  averageSpeedMbps: number;
  stabilityScore: number;
}

export interface LatencyMetrics {
  pingMs: number;
  jitterMs: number;
  packetLossPercent: number;
  dnsLookupMs: number;
  tcpConnectMs: number;
  sslHandshakeMs: number;
  firstByteMs: number;
}

export interface StatisticalSummary {
  downloadStats: Statistics;
  uploadStats: Statistics;
  latencyStats: Statistics;
}

export interface Statistics {
  median: number;
  mean: number;
  min: number;
  max: number;
  percentile95: number;
  percentile99: number;
  standardDeviation: number;
}

export interface HistoryItem {
  resultId: string;
  testTimestamp: string;
  downloadSpeedMbps: number;
  uploadSpeedMbps: number;
  latencyMs: number;
  location: string;
  serverProvider: string;
}

export interface TimeSeriesData {
  downloadSeries: DataPoint[];
  uploadSeries: DataPoint[];
  latencySeries: DataPoint[];
}

export interface DataPoint {
  timestamp: string;
  value: number;
  label?: string;
}

@Injectable({
  providedIn: 'root'
})
export class SpeedTestService {
  private baseUrl = 'http://localhost:8080/api/speedtest';
  private currentUserId = 'demo-user-1'; // In real app, get from auth service

  constructor(private http: HttpClient) {}

  /**
   * Performs a complete client-side speed test and returns results to be saved
   */
  async performClientSideSpeedTest(request: SpeedTestRequest): Promise<SpeedTestResponse> {
    const sessionId = this.generateSessionId();
    const response: SpeedTestResponse = {
      sessionId,
      testTimestamp: new Date().toISOString(),
      status: 'RUNNING',
      currentPhase: 'INITIALIZATION',
      progressPercentage: 0
    };

    try {
      // Run tests based on test type
      if (request.testType === 'FULL' || request.testType === 'LATENCY_ONLY') {
        response.currentPhase = 'LATENCY_TEST';
        response.latencyMetrics = await this.performLatencyTest();
      }

      if (request.testType === 'FULL' || request.testType === 'DOWNLOAD_ONLY') {
        response.currentPhase = 'DOWNLOAD_TEST';
        response.downloadMetrics = await this.performDownloadTest(request.testDurationSeconds);
      }

      if (request.testType === 'FULL' || request.testType === 'UPLOAD_ONLY') {
        response.currentPhase = 'UPLOAD_TEST';
        response.uploadMetrics = await this.performUploadTest(request.testDurationSeconds);
      }

      response.currentPhase = 'COMPLETED';
      response.status = 'COMPLETED';
      response.progressPercentage = 100;

      // Save results to backend
      await this.saveResults(response);

      return response;

    } catch (error) {
      response.status = 'FAILED';
      response.errorMessage = error instanceof Error ? error.message : 'Unknown error';
      throw error;
    }
  }

  /**
   * Client-side download speed test with improved stability
   */
  private async performDownloadTest(durationSeconds: number): Promise<SpeedMetrics> {
    const fileSizeMB = Math.max(50, durationSeconds * 15); // Enough data for the test duration
    const url = `${this.baseUrl}/download/${fileSizeMB}`;

    console.log(`Starting download test: ${fileSizeMB} MB file over ${durationSeconds} seconds`);

    const startTime = performance.now();
    let totalBytes = 0;
    let peakSpeed = 0;
    const speeds: number[] = [];
    const smoothedSpeeds: number[] = []; // For better stability calculation

    try {
      const response = await fetch(url, {
        method: 'GET',
        cache: 'no-store'
      });

      if (!response.body) {
        throw new Error('No response body');
      }

      const reader = response.body.getReader();
      const testStartTime = performance.now();
      let lastMeasurementTime = testStartTime;
      let bytesSinceLastMeasurement = 0;
      let lastLogTime = testStartTime;

      // Moving average window for smoothing
      const speedWindow: number[] = [];
      const windowSize = 3; // Average over last 3 measurements

      while (true) {
        const currentTime = performance.now();
        const elapsedSeconds = (currentTime - testStartTime) / 1000;

        // Stop after specified duration
        if (elapsedSeconds >= durationSeconds) {
          console.log(`Download test duration reached: ${elapsedSeconds.toFixed(2)}s`);
          break;
        }

        const { done, value } = await reader.read();
        if (done) {
          console.log('Download completed (all data received)');
          break;
        }

        const bytesRead = value?.length || 0;
        totalBytes += bytesRead;
        bytesSinceLastMeasurement += bytesRead;

        // Calculate instantaneous speed every 250ms for more stable readings
        const timeSinceLastMeasurement = currentTime - lastMeasurementTime;
        if (timeSinceLastMeasurement >= 250) {
          const instantSpeed = (bytesSinceLastMeasurement * 8) / (1024 * 1024) / (timeSinceLastMeasurement / 1000);

          // Add to moving average window
          speedWindow.push(instantSpeed);
          if (speedWindow.length > windowSize) {
            speedWindow.shift();
          }

          // Calculate smoothed speed (moving average)
          const smoothedSpeed = speedWindow.reduce((a, b) => a + b, 0) / speedWindow.length;
          speeds.push(instantSpeed);
          smoothedSpeeds.push(smoothedSpeed);
          peakSpeed = Math.max(peakSpeed, smoothedSpeed);

          bytesSinceLastMeasurement = 0;
          lastMeasurementTime = currentTime;

          // Log progress every second
          if (currentTime - lastLogTime >= 1000) {
            console.log(`Download progress: ${(totalBytes / 1024 / 1024).toFixed(2)} MB (${smoothedSpeed.toFixed(2)} Mbps)`);
            lastLogTime = currentTime;
          }
        }
      }

      reader.cancel();

    } catch (error) {
      console.error('Download test error:', error);
    }

    const endTime = performance.now();
    const durationMs = endTime - startTime;
    const actualDurationSeconds = durationMs / 1000;

    // Calculate average speed from smoothed values
    const averageSpeedMbps = smoothedSpeeds.length > 0
      ? smoothedSpeeds.reduce((a, b) => a + b, 0) / smoothedSpeeds.length
      : totalBytes > 0 ? (totalBytes * 8) / (1024 * 1024) / actualDurationSeconds : 0;

    // Calculate stability using smoothed speeds (lower variance = higher stability)
    const variance = smoothedSpeeds.length > 0
      ? smoothedSpeeds.reduce((sum, speed) => sum + Math.pow(speed - averageSpeedMbps, 2), 0) / smoothedSpeeds.length
      : 0;

    // Improved stability score calculation
    const coefficientOfVariation = averageSpeedMbps > 0 ? Math.sqrt(variance) / averageSpeedMbps : 0;
    const stabilityScore = Math.max(0, Math.min(100, 100 - (coefficientOfVariation * 100)));

    console.log(`Download test completed: ${averageSpeedMbps.toFixed(2)} Mbps (peak: ${peakSpeed.toFixed(2)} Mbps), ${(totalBytes / 1024 / 1024).toFixed(2)} MB in ${actualDurationSeconds.toFixed(2)}s, stability: ${stabilityScore.toFixed(0)}%`);

    return {
      speedMbps: averageSpeedMbps,
      bytesTransferred: totalBytes,
      durationSeconds: actualDurationSeconds,
      peakSpeedMbps: peakSpeed,
      averageSpeedMbps: averageSpeedMbps,
      stabilityScore: stabilityScore
    };
  }

  /**
   * Client-side upload speed test using XMLHttpRequest for better upload tracking
   */
  private async performUploadTest(durationSeconds: number): Promise<SpeedMetrics> {
    const totalDataSize = durationSeconds * 5 * 1024 * 1024; // 5 MB per second
    const url = `${this.baseUrl}/upload`;

    return new Promise((resolve) => {
      const startTime = performance.now();
      let peakSpeed = 0;
      const speeds: number[] = [];
      let lastProgressTime = startTime;
      let lastLoadedBytes = 0;

      // Generate test data in chunks (crypto.getRandomValues has a 64KB limit)
      const testData = new Uint8Array(totalDataSize);
      const chunkSize = 65536; // 64 KB - max for crypto.getRandomValues
      for (let i = 0; i < totalDataSize; i += chunkSize) {
        const size = Math.min(chunkSize, totalDataSize - i);
        const chunk = new Uint8Array(size);
        crypto.getRandomValues(chunk);
        testData.set(chunk, i);
      }

      console.log(`Starting upload test: ${(totalDataSize / 1024 / 1024).toFixed(2)} MB over ${durationSeconds} seconds`);

      const xhr = new XMLHttpRequest();
      xhr.open('POST', url, true);
      xhr.setRequestHeader('Content-Type', 'application/octet-stream');

      // Moving average for smoothing
      const speedWindow: number[] = [];
      const windowSize = 3;

      // Track upload progress with improved stability
      xhr.upload.addEventListener('progress', (event) => {
        const currentTime = performance.now();
        const elapsedTime = (currentTime - lastProgressTime) / 1000;

        // Measure every 250ms for more stable readings
        if (event.lengthComputable && elapsedTime >= 0.25) {
          const bytesSinceLastUpdate = event.loaded - lastLoadedBytes;
          const instantSpeed = (bytesSinceLastUpdate * 8) / (1024 * 1024) / elapsedTime;

          // Add to moving average window
          speedWindow.push(instantSpeed);
          if (speedWindow.length > windowSize) {
            speedWindow.shift();
          }

          // Calculate smoothed speed
          const smoothedSpeed = speedWindow.reduce((a, b) => a + b, 0) / speedWindow.length;
          speeds.push(smoothedSpeed);
          peakSpeed = Math.max(peakSpeed, smoothedSpeed);

          lastProgressTime = currentTime;
          lastLoadedBytes = event.loaded;

          console.log(`Upload progress: ${(event.loaded / 1024 / 1024).toFixed(2)} MB / ${(event.total / 1024 / 1024).toFixed(2)} MB (${smoothedSpeed.toFixed(2)} Mbps)`);
        }
      });

      xhr.upload.addEventListener('load', () => {
        console.log('Upload complete');
      });

      xhr.addEventListener('load', () => {
        const endTime = performance.now();
        const durationMs = endTime - startTime;
        const actualDurationSeconds = durationMs / 1000;

        const totalBytes = testData.length;

        // Calculate average speed from smoothed measurements
        const averageSpeedMbps = speeds.length > 0
          ? speeds.reduce((a, b) => a + b, 0) / speeds.length
          : (totalBytes * 8) / (1024 * 1024) / actualDurationSeconds;

        // Calculate stability using smoothed speeds
        const variance = speeds.length > 0
          ? speeds.reduce((sum, speed) => sum + Math.pow(speed - averageSpeedMbps, 2), 0) / speeds.length
          : 0;

        // Improved stability score calculation
        const coefficientOfVariation = averageSpeedMbps > 0 ? Math.sqrt(variance) / averageSpeedMbps : 0;
        const stabilityScore = Math.max(0, Math.min(100, 100 - (coefficientOfVariation * 100)));

        console.log(`Upload test completed: ${averageSpeedMbps.toFixed(2)} Mbps (peak: ${peakSpeed.toFixed(2)} Mbps), stability: ${stabilityScore.toFixed(0)}%`);

        resolve({
          speedMbps: averageSpeedMbps,
          bytesTransferred: totalBytes,
          durationSeconds: actualDurationSeconds,
          peakSpeedMbps: peakSpeed,
          averageSpeedMbps: averageSpeedMbps,
          stabilityScore: stabilityScore
        });
      });

      xhr.addEventListener('error', (error) => {
        console.error('Upload test failed:', error);
        resolve({
          speedMbps: 0,
          bytesTransferred: 0,
          durationSeconds: 0,
          peakSpeedMbps: 0,
          averageSpeedMbps: 0,
          stabilityScore: 0
        });
      });

      // Send the data
      xhr.send(testData);
    });
  }

  /**
   * Client-side latency test
   */
  private async performLatencyTest(): Promise<LatencyMetrics> {
    const pingCount = 10;
    const pings: number[] = [];
    const url = `${this.baseUrl}/health`;

    for (let i = 0; i < pingCount; i++) {
      const startTime = performance.now();

      try {
        await fetch(url, {
          method: 'GET',
          cache: 'no-store'
        });
      } catch (error) {
        console.error('Ping failed:', error);
      }

      const endTime = performance.now();
      pings.push(endTime - startTime);

      // Small delay between pings
      await new Promise(resolve => setTimeout(resolve, 100));
    }

    const avgPing = pings.reduce((a, b) => a + b, 0) / pings.length;
    const jitter = Math.sqrt(pings.reduce((sum, ping) => sum + Math.pow(ping - avgPing, 2), 0) / pings.length);

    return {
      pingMs: avgPing,
      jitterMs: jitter,
      packetLossPercent: 0, // Could implement by tracking failed pings
      dnsLookupMs: 0,
      tcpConnectMs: 0,
      sslHandshakeMs: 0,
      firstByteMs: avgPing
    };
  }

  /**
   * Save results to backend for validation and storage
   */
  private async saveResults(results: SpeedTestResponse): Promise<void> {
    try {
      await this.http.post(`${this.baseUrl}/results`, results).toPromise();
    } catch (error) {
      console.error('Failed to save results:', error);
      // Don't throw - we still want to show results even if save fails
    }
  }

  private generateSessionId(): string {
    return `session_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }

  // Legacy methods for backward compatibility
  getUserHistory(limit: number = 10): Observable<HistoryItem[]> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<HistoryItem[]>(`${this.baseUrl}/history`, { params });
  }

  getTimeSeriesData(startDate: string, endDate: string): Observable<TimeSeriesData> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);
    return this.http.get<TimeSeriesData>(`${this.baseUrl}/timeseries`, { params });
  }

  healthCheck(): Observable<string> {
    return this.http.get(`${this.baseUrl}/health`, { responseType: 'text' });
  }
}