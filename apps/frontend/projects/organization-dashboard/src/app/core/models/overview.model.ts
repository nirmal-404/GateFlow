export type DateRange = '24h' | '7d' | '30d';

export interface KpiMetric {
  id: string;
  title: string;
  value: string;
  change: string;
  isPositive: boolean;
  periodLabel: string;
  icon: 'requests' | 'success' | 'latency' | 'keys';
  supportingText?: string;
}

export interface TrafficSeries {
  categories: string[];
  total: number[];
  successful: number[];
  failed: number[];
}

export interface TopApi {
  id: string;
  name: string;
  path: string;
  method: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';
  requests: string;
  requestsCount: number;
  successRate: number;
  avgLatencyMs: number;
}

export type ActivityType = 'key_created' | 'app_registered' | 'sub_activated' | 'key_revoked' | 'policy_updated';

export interface ActivityItem {
  id: string;
  type: ActivityType;
  title: string;
  description: string;
  timestamp: string;
  user: string;
  badgeLabel: string;
  badgeType: 'success' | 'info' | 'warning' | 'danger';
}
