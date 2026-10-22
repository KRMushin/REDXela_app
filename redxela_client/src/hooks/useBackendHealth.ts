import { useState, useEffect, useCallback } from 'react';
import type { BackendStatus, HealthResponse } from '../types/health';
import { checkBackendHealth } from '../services/healthService';

const POLL_INTERVAL_MS = 15_000;

interface BackendHealthState {
  status: BackendStatus;
  data: HealthResponse | null;
}

export function useBackendHealth(): BackendHealthState {
  const [status, setStatus] = useState<BackendStatus>('checking');
  const [data, setData] = useState<HealthResponse | null>(null);

  const check = useCallback(async () => {
    try {
      const response = await checkBackendHealth();
      setData(response);
      setStatus('connected');
    } catch {
      setData(null);
      setStatus('disconnected');
    }
  }, []);

  useEffect(() => {
    void check();
    const intervalId = setInterval(() => void check(), POLL_INTERVAL_MS);
    return () => clearInterval(intervalId);
  }, [check]);

  return { status, data };
}
