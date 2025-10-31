/// <reference lib="webworker" />

/**
 * Web Worker for continuous latency testing
 * Maintains a persistent WebSocket connection and sends pings every second
 */

// Get worker context
const ctx: DedicatedWorkerGlobalScope = self as any;

export interface LatencyWorkerMessage {
  type: 'start' | 'stop' | 'pause' | 'resume' | 'config';
  baseUrl?: string;
  pingInterval?: number; // milliseconds
}

export interface LatencyMetrics {
  pingMs: number;
  jitterMs: number;
  packetLossPercent: number;
  timestamp: number;
  connectionState: 'connected' | 'connecting' | 'disconnected' | 'reconnecting';
}

export interface LatencyWorkerResponse {
  type: 'metrics' | 'error' | 'status';
  data?: LatencyMetrics;
  message?: string;
  connectionState?: string;
}

// Worker state
let ws: WebSocket | null = null;
let pingInterval: number = 1000; // 1 second
let pingTimer: any = null;
let isRunning: boolean = false;
let isPaused: boolean = false;
let baseUrl: string = 'ws://localhost:8080/api/speedtest';
let reconnectAttempts: number = 0;
let maxReconnectAttempts: number = 5;
let reconnectDelay: number = 2000; // Start with 2 seconds
let reconnectTimer: any = null;

// Metrics tracking
let pingHistory: number[] = [];
let maxHistorySize: number = 60; // Keep last 60 pings for jitter calculation
let lastPingTime: number = 0;
let pendingPings: Map<number, number> = new Map(); // timestamp -> startTime
let consecutiveFailures: number = 0;
let totalPingsSent: number = 0;
let totalPingsReceived: number = 0;

/**
 * Initialize WebSocket connection
 * Connection establishment time is NOT included in latency metrics
 */
function connectWebSocket(): void {
  if (ws) {
    ws.close();
    ws = null;
  }

  const wsUrl = baseUrl.replace('http://', 'ws://').replace('https://', 'wss://') + '/ws/ping';

  ctx.postMessage({
    type: 'status',
    message: reconnectAttempts > 0 ? 'reconnecting' : 'connecting',
    connectionState: reconnectAttempts > 0 ? 'reconnecting' : 'connecting'
  } as LatencyWorkerResponse);

  try {
    ws = new WebSocket(wsUrl);

    // Connection timeout
    const connectionTimeout = setTimeout(() => {
      if (ws && ws.readyState !== WebSocket.OPEN) {
        console.error('[LatencyWorker] Connection timeout');
        ws.close();
        handleConnectionError();
      }
    }, 5000);

    ws.onopen = () => {
      clearTimeout(connectionTimeout);
      console.log('[LatencyWorker] WebSocket connection established');

      reconnectAttempts = 0;
      reconnectDelay = 2000;
      consecutiveFailures = 0;

      ctx.postMessage({
        type: 'status',
        message: 'connected',
        connectionState: 'connected'
      } as LatencyWorkerResponse);

      // Start sending pings if running and not paused
      if (isRunning && !isPaused) {
        startPinging();
      }
    };

    ws.onmessage = (event: MessageEvent) => {
      handlePongMessage(event.data);
    };

    ws.onerror = (error) => {
      console.error('[LatencyWorker] WebSocket error:', error);
    };

    ws.onclose = (event) => {
      console.log('[LatencyWorker] WebSocket closed:', event.code, event.reason);

      ctx.postMessage({
        type: 'status',
        message: 'disconnected',
        connectionState: 'disconnected'
      } as LatencyWorkerResponse);

      // Attempt to reconnect if still running
      if (isRunning && !isPaused) {
        handleConnectionError();
      }
    };

  } catch (error) {
    console.error('[LatencyWorker] Failed to create WebSocket:', error);
    ctx.postMessage({
      type: 'error',
      message: `Failed to create WebSocket: ${error}`
    } as LatencyWorkerResponse);
    handleConnectionError();
  }
}

/**
 * Handle connection errors and reconnection logic
 */
function handleConnectionError(): void {
  stopPinging();

  if (!isRunning) {
    return;
  }

  if (reconnectAttempts < maxReconnectAttempts) {
    reconnectAttempts++;
    const delay = reconnectDelay * Math.min(reconnectAttempts, 3); // Exponential backoff, capped at 3x

    console.log(`[LatencyWorker] Reconnecting in ${delay}ms (attempt ${reconnectAttempts}/${maxReconnectAttempts})`);

    ctx.postMessage({
      type: 'status',
      message: `Reconnecting in ${delay}ms (attempt ${reconnectAttempts}/${maxReconnectAttempts})`,
      connectionState: 'reconnecting'
    } as LatencyWorkerResponse);

    reconnectTimer = setTimeout(() => {
      connectWebSocket();
    }, delay);
  } else {
    console.error('[LatencyWorker] Max reconnection attempts reached');
    ctx.postMessage({
      type: 'error',
      message: 'Failed to connect after multiple attempts. Please check your connection.'
    } as LatencyWorkerResponse);
    stop();
  }
}

/**
 * Start sending periodic pings
 */
function startPinging(): void {
  stopPinging(); // Clear any existing timer

  if (!ws || ws.readyState !== WebSocket.OPEN) {
    console.warn('[LatencyWorker] Cannot start pinging: WebSocket not open');
    return;
  }

  console.log(`[LatencyWorker] Starting pings every ${pingInterval}ms`);

  // Send first ping immediately
  sendPing();

  // Then continue at specified interval
  pingTimer = setInterval(() => {
    sendPing();
  }, pingInterval);
}

/**
 * Stop sending pings
 */
function stopPinging(): void {
  if (pingTimer) {
    clearInterval(pingTimer);
    pingTimer = null;
  }
}

/**
 * Send a single ping
 */
function sendPing(): void {
  if (!ws || ws.readyState !== WebSocket.OPEN) {
    console.warn('[LatencyWorker] Cannot send ping: WebSocket not open');
    consecutiveFailures++;

    // If multiple consecutive failures, try to reconnect
    if (consecutiveFailures >= 3) {
      console.error('[LatencyWorker] Multiple consecutive ping failures, reconnecting...');
      connectWebSocket();
    }
    return;
  }

  const startTime = performance.now();
  const timestamp = Date.now();

  // Store pending ping
  pendingPings.set(timestamp, startTime);
  totalPingsSent++;

  try {
    ws.send(`PING:${startTime}`);
    lastPingTime = startTime;
  } catch (error) {
    console.error('[LatencyWorker] Failed to send ping:', error);
    pendingPings.delete(timestamp);
    consecutiveFailures++;

    if (consecutiveFailures >= 3) {
      connectWebSocket();
    }
  }

  // Clean up old pending pings (older than 5 seconds)
  const now = Date.now();
  for (const [ts, _] of pendingPings.entries()) {
    if (now - ts > 5000) {
      pendingPings.delete(ts);
    }
  }
}

/**
 * Handle PONG response from server
 */
function handlePongMessage(data: string): void {
  if (!data || !(data === 'PONG' || data.startsWith('PONG:'))) {
    return;
  }

  const endTime = performance.now();
  let startTime: number = 0;

  // Parse the timestamp from the message
  if (data.startsWith('PONG:')) {
    startTime = parseFloat(data.split(':')[1]);
  }

  if (startTime <= 0) {
    console.warn('[LatencyWorker] Invalid PONG message:', data);
    return;
  }

  // Calculate latency (RTT)
  const latency = endTime - startTime;

  // Remove from pending pings
  const timestamp = Array.from(pendingPings.entries())
    .find(([_, time]) => time === startTime)?.[0];

  if (timestamp) {
    pendingPings.delete(timestamp);
  }

  totalPingsReceived++;
  consecutiveFailures = 0; // Reset failure counter

  // Add to history
  pingHistory.push(latency);
  if (pingHistory.length > maxHistorySize) {
    pingHistory.shift();
  }

  // Calculate metrics
  const metrics = calculateMetrics(latency);

  // Send metrics to main thread
  ctx.postMessage({
    type: 'metrics',
    data: metrics
  } as LatencyWorkerResponse);
}

/**
 * Calculate latency metrics
 */
function calculateMetrics(currentPing: number): LatencyMetrics {
  const avgPing = pingHistory.length > 0
    ? pingHistory.reduce((a, b) => a + b, 0) / pingHistory.length
    : currentPing;

  // Calculate jitter (standard deviation)
  const jitter = pingHistory.length > 1
    ? Math.sqrt(
        pingHistory.reduce((sum, ping) => sum + Math.pow(ping - avgPing, 2), 0) / pingHistory.length
      )
    : 0;

  // Calculate packet loss
  const packetLoss = totalPingsSent > 0
    ? ((totalPingsSent - totalPingsReceived) / totalPingsSent) * 100
    : 0;

  const connectionState = ws?.readyState === WebSocket.OPEN
    ? 'connected'
    : ws?.readyState === WebSocket.CONNECTING
    ? 'connecting'
    : reconnectTimer
    ? 'reconnecting'
    : 'disconnected';

  return {
    pingMs: avgPing,
    jitterMs: jitter,
    packetLossPercent: Math.max(0, Math.min(100, packetLoss)),
    timestamp: Date.now(),
    connectionState: connectionState as any
  };
}

/**
 * Start the worker
 */
function start(url?: string): void {
  if (isRunning) {
    console.warn('[LatencyWorker] Already running');
    return;
  }

  console.log('[LatencyWorker] Starting continuous latency monitoring');

  if (url) {
    baseUrl = url;
  }

  isRunning = true;
  isPaused = false;
  reconnectAttempts = 0;
  pingHistory = [];
  pendingPings.clear();
  consecutiveFailures = 0;
  totalPingsSent = 0;
  totalPingsReceived = 0;

  connectWebSocket();
}

/**
 * Stop the worker
 */
function stop(): void {
  console.log('[LatencyWorker] Stopping');

  isRunning = false;
  isPaused = false;

  stopPinging();

  if (reconnectTimer) {
    clearTimeout(reconnectTimer);
    reconnectTimer = null;
  }

  if (ws) {
    ws.close();
    ws = null;
  }

  // Reset state
  reconnectAttempts = 0;
  pingHistory = [];
  pendingPings.clear();
  consecutiveFailures = 0;

  ctx.postMessage({
    type: 'status',
    message: 'stopped',
    connectionState: 'disconnected'
  } as LatencyWorkerResponse);
}

/**
 * Pause pinging (keep connection alive)
 */
function pause(): void {
  if (!isRunning) {
    return;
  }

  console.log('[LatencyWorker] Pausing');
  isPaused = true;
  stopPinging();

  ctx.postMessage({
    type: 'status',
    message: 'paused',
    connectionState: ws?.readyState === WebSocket.OPEN ? 'connected' : 'disconnected'
  } as LatencyWorkerResponse);
}

/**
 * Resume pinging
 */
function resume(): void {
  if (!isRunning) {
    return;
  }

  console.log('[LatencyWorker] Resuming');
  isPaused = false;

  // Reconnect if disconnected
  if (!ws || ws.readyState !== WebSocket.OPEN) {
    connectWebSocket();
  } else {
    startPinging();
  }

  ctx.postMessage({
    type: 'status',
    message: 'resumed',
    connectionState: 'connected'
  } as LatencyWorkerResponse);
}

/**
 * Update configuration
 */
function updateConfig(interval?: number): void {
  if (interval && interval > 0) {
    pingInterval = interval;

    // Restart pinging with new interval if currently running
    if (isRunning && !isPaused && ws?.readyState === WebSocket.OPEN) {
      startPinging();
    }

    console.log(`[LatencyWorker] Ping interval updated to ${pingInterval}ms`);
  }
}

/**
 * Message handler
 */
ctx.addEventListener('message', ({ data }: MessageEvent<LatencyWorkerMessage>) => {
  switch (data.type) {
    case 'start':
      start(data.baseUrl);
      break;
    case 'stop':
      stop();
      break;
    case 'pause':
      pause();
      break;
    case 'resume':
      resume();
      break;
    case 'config':
      updateConfig(data.pingInterval);
      break;
    default:
      console.warn('[LatencyWorker] Unknown message type:', data.type);
  }
});

// Log worker initialization
console.log('[LatencyWorker] Initialized');
