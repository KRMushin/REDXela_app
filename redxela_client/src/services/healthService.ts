import { api } from './api';
import type { HealthResponse } from '../types/health';

export async function checkBackendHealth(): Promise<HealthResponse> {
  return api.get<HealthResponse>('/health');
}
