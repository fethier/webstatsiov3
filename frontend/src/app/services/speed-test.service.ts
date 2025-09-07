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

  startSpeedTest(request: SpeedTestRequest): Observable<SpeedTestResponse> {
    // For anonymous usage, don't send userId parameter
    // const params = new HttpParams().set('userId', this.currentUserId);
    return this.http.post<SpeedTestResponse>(`${this.baseUrl}/start`, request);
  }

  getTestStatus(sessionId: string): Observable<SpeedTestResponse> {
    return this.http.get<SpeedTestResponse>(`${this.baseUrl}/status/${sessionId}`);
  }

  pollTestStatus(sessionId: string): Observable<SpeedTestResponse> {
    return interval(1000).pipe(
      switchMap(() => this.getTestStatus(sessionId)),
      takeWhile(response => 
        response.status === 'INITIALIZING' || response.status === 'RUNNING', 
        true
      )
    );
  }

  getUserHistory(limit: number = 10): Observable<HistoryItem[]> {
    // For anonymous users, don't send userId parameter - server will default to 'anonymous'
    const params = new HttpParams()
      .set('limit', limit.toString());
    return this.http.get<HistoryItem[]>(`${this.baseUrl}/history`, { params });
  }

  getTimeSeriesData(startDate: string, endDate: string): Observable<TimeSeriesData> {
    // For anonymous users, don't send userId parameter - server will default to 'anonymous'
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);
    return this.http.get<TimeSeriesData>(`${this.baseUrl}/timeseries`, { params });
  }

  healthCheck(): Observable<string> {
    return this.http.get(`${this.baseUrl}/health`, { responseType: 'text' });
  }
}