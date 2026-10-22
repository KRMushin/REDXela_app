export interface HealthResponse {
  status: string;
  application: string;
  message: string;
}

export type BackendStatus = 'checking' | 'connected' | 'disconnected';
