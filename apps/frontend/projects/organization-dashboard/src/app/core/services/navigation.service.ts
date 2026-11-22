import { Injectable, signal } from '@angular/core';

export interface Organization {
  id: string;
  name: string;
  slug: string;
  plan: 'Enterprise' | 'Team' | 'Developer';
  avatar?: string;
}

export interface NavItem {
  label: string;
  route: string;
  icon: string;
  badge?: string;
}

@Injectable({
  providedIn: 'root'
})
export class NavigationService {
  readonly isMobileSidebarOpen = signal<boolean>(false);

  readonly availableOrganizations = signal<Organization[]>([
    { id: 'org-1', name: 'Acme Corporation', slug: 'acme-corp', plan: 'Enterprise' },
    { id: 'org-2', name: 'Staging Environment', slug: 'acme-staging', plan: 'Team' },
    { id: 'org-3', name: 'Sandbox Project', slug: 'sandbox-dev', plan: 'Developer' }
  ]);

  readonly currentOrganization = signal<Organization>(this.availableOrganizations()[0]);

  readonly navItems: NavItem[] = [
    { label: 'Overview', route: '/overview', icon: 'overview' },
    { label: 'Applications', route: '/applications', icon: 'applications' },
    { label: 'APIs', route: '/apis', icon: 'apis' },
    { label: 'API Keys', route: '/api-keys', icon: 'api-keys' },
    { label: 'Analytics', route: '/analytics', icon: 'analytics' },
    { label: 'Team', route: '/team', icon: 'team' },
    { label: 'Settings', route: '/settings', icon: 'settings' }
  ];

  toggleMobileSidebar(): void {
    this.isMobileSidebarOpen.update(open => !open);
  }

  closeMobileSidebar(): void {
    this.isMobileSidebarOpen.set(false);
  }

  openMobileSidebar(): void {
    this.isMobileSidebarOpen.set(true);
  }

  selectOrganization(org: Organization): void {
    this.currentOrganization.set(org);
  }
}
