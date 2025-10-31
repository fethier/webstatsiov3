import { Injectable, OnDestroy } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface LatencyMetrics {
  pingMs: number;
  jitterMs: number;
  packetLossPercent: number;
  timestamp: number;
  connectionState: 'connected' | 'connecting' | 'disconnected' | 'reconnecting';
}

export interface WorkerStatus {
  isRunning: boolean;
  isPaused: boolean;
  connectionState: string;
  message?: string;
}

@Injectable({
  providedIn: 'root'
})
export class LatencyWorkerService implements OnDestroy {
  private worker: Worker | null = null;
  private metricsSubject = new BehaviorSubject<LatencyMetrics | null>(null);
  private statusSubject = new BehaviorSubject<WorkerStatus>({
    isRunning: false,
    isPaused: false,
    connectionState: 'disconnected'
  });
  private errorSubject = new BehaviorSubject<string | null>(null);

  // Observables
  public metrics$: Observable<LatencyMetrics | null> = this.metricsSubject.asObservable();
  public status$: Observable<WorkerStatus> = this.statusSubject.asObservable();
  public error$: Observable<string | null> = this.errorSubject.asObservable();

  // Page Visibility API integration
  private visibilityChangeHandler: (() => void) | null = null;
  private wasRunningBeforeHidden = false;

  constructor() {
    this.setupVisibilityListener();
  }

  /**
   * Start continuous latency monitoring
   */
  public start(baseUrl: string = 'http://localhost:8080/api/speedtest', pingInterval: number = 1000): void {
    if (this.worker) {
      console.warn('[LatencyWorkerService] Worker already running');
      return;
    }

    console.log('[LatencyWorkerService] Starting latency worker');

    try {
      // Create worker - Angular CLI will handle the bundling
      this.worker = new Worker(new URL('../workers/latency-worker', import.meta.url), {
        type: 'module'
      });

      // Set up message handler
      this.worker.onmessage = ({ data }) => {
        this.handleWorkerMessage(data);
      };

      // Set up error handler
      this.worker.onerror = (error) => {
        console.error('[LatencyWorkerService] Worker error:', error);
        this.errorSubject.next(`Worker error: ${error.message}`);
      };

      // Send start message to worker
      this.worker.postMessage({
        type: 'start',
        baseUrl: baseUrl,
        pingInterval: pingInterval
      });

      // Update status
      this.statusSubject.next({
        isRunning: true,
        isPaused: false,
        connectionState: 'connecting'
      });

    } catch (error) {
      console.error('[LatencyWorkerService] Failed to create worker:', error);
      this.errorSubject.next(`Failed to create worker: ${error}`);
      this.worker = null;
    }
  }

  /**
   * Stop continuous latency monitoring
   */
  public stop(): void {
    if (!this.worker) {
      return;
    }

    console.log('[LatencyWorkerService] Stopping latency worker');

    // Send stop message
    this.worker.postMessage({ type: 'stop' });

    // Terminate worker
    this.worker.terminate();
    this.worker = null;

    // Reset state
    this.metricsSubject.next(null);
    this.statusSubject.next({
      isRunning: false,
      isPaused: false,
      connectionState: 'disconnected'
    });
  }

  /**
   * Pause latency monitoring (keeps connection alive but stops pinging)
   */
  public pause(): void {
    if (!this.worker) {
      return;
    }

    console.log('[LatencyWorkerService] Pausing latency worker');
    this.worker.postMessage({ type: 'pause' });

    const currentStatus = this.statusSubject.value;
    this.statusSubject.next({
      ...currentStatus,
      isPaused: true
    });
  }

  /**
   * Resume latency monitoring
   */
  public resume(): void {
    if (!this.worker) {
      return;
    }

    console.log('[LatencyWorkerService] Resuming latency worker');
    this.worker.postMessage({ type: 'resume' });

    const currentStatus = this.statusSubject.value;
    this.statusSubject.next({
      ...currentStatus,
      isPaused: false
    });
  }

  /**
   * Update ping interval
   */
  public updateInterval(pingInterval: number): void {
    if (!this.worker) {
      return;
    }

    console.log(`[LatencyWorkerService] Updating ping interval to ${pingInterval}ms`);
    this.worker.postMessage({
      type: 'config',
      pingInterval: pingInterval
    });
  }

  /**
   * Get current metrics (synchronous access to latest value)
   */
  public getCurrentMetrics(): LatencyMetrics | null {
    return this.metricsSubject.value;
  }

  /**
   * Get current status (synchronous access to latest value)
   */
  public getCurrentStatus(): WorkerStatus {
    return this.statusSubject.value;
  }

  /**
   * Check if worker is currently running
   */
  public isRunning(): boolean {
    return this.statusSubject.value.isRunning;
  }

  /**
   * Handle messages from worker
   */
  private handleWorkerMessage(data: any): void {
    switch (data.type) {
      case 'metrics':
        if (data.data) {
          this.metricsSubject.next(data.data);

          // Update connection state in status
          const currentStatus = this.statusSubject.value;
          this.statusSubject.next({
            ...currentStatus,
            connectionState: data.data.connectionState || 'connected'
          });
        }
        break;

      case 'status':
        const currentStatus = this.statusSubject.value;
        this.statusSubject.next({
          ...currentStatus,
          connectionState: data.connectionState || data.message || 'unknown',
          message: data.message
        });
        break;

      case 'error':
        this.errorSubject.next(data.message || 'Unknown error');
        break;

      default:
        console.warn('[LatencyWorkerService] Unknown message type:', data.type);
    }
  }

  /**
   * Set up Page Visibility API listener
   * Pauses worker when tab is hidden, resumes when visible
   */
  private setupVisibilityListener(): void {
    if (typeof document === 'undefined') {
      return;
    }

    this.visibilityChangeHandler = () => {
      if (document.hidden) {
        // Tab became hidden
        console.log('[LatencyWorkerService] Tab hidden, checking if should pause...');

        const status = this.statusSubject.value;
        if (status.isRunning && !status.isPaused) {
          console.log('[LatencyWorkerService] Pausing worker due to tab visibility');
          this.wasRunningBeforeHidden = true;
          // Note: We don't pause because browser will throttle to 1000ms anyway
          // and we want the worker to continue monitoring
          // If you want to pause when hidden, uncomment:
          // this.pause();
        }
      } else {
        // Tab became visible
        console.log('[LatencyWorkerService] Tab visible');

        const status = this.statusSubject.value;
        if (this.wasRunningBeforeHidden && status.isRunning && status.isPaused) {
          console.log('[LatencyWorkerService] Resuming worker after tab visibility');
          this.resume();
          this.wasRunningBeforeHidden = false;
        }
      }
    };

    document.addEventListener('visibilitychange', this.visibilityChangeHandler);
  }

  /**
   * Clean up on service destroy
   */
  ngOnDestroy(): void {
    console.log('[LatencyWorkerService] Destroying service');

    // Remove visibility listener
    if (this.visibilityChangeHandler && typeof document !== 'undefined') {
      document.removeEventListener('visibilitychange', this.visibilityChangeHandler);
    }

    // Stop worker
    this.stop();

    // Complete subjects
    this.metricsSubject.complete();
    this.statusSubject.complete();
    this.errorSubject.complete();
  }
}
