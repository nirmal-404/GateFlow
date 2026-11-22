import { Routes } from '@angular/router';
import { LayoutComponent } from './layout/layout.component';
import { OverviewComponent } from './pages/overview/overview.component';
import { PlaceholderComponent } from './pages/placeholder/placeholder.component';

export const routes: Routes = [
  {
    path: '',
    component: LayoutComponent,
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'overview'
      },
      {
        path: 'overview',
        component: OverviewComponent,
        title: 'Overview – GateFlow'
      },
      {
        path: 'applications',
        component: PlaceholderComponent,
        data: { title: 'Applications', description: 'Manage connected client applications and permissions.' },
        title: 'Applications – GateFlow'
      },
      {
        path: 'apis',
        component: PlaceholderComponent,
        data: { title: 'APIs', description: 'Manage managed API services, endpoints, and schemas.' },
        title: 'APIs – GateFlow'
      },
      {
        path: 'api-keys',
        component: PlaceholderComponent,
        data: { title: 'API Keys', description: 'Create and revoke organization API keys and tokens.' },
        title: 'API Keys – GateFlow'
      },
      {
        path: 'analytics',
        component: PlaceholderComponent,
        data: { title: 'Analytics', description: 'Traffic, error rates, and latency analytics.' },
        title: 'Analytics – GateFlow'
      },
      {
        path: 'team',
        component: PlaceholderComponent,
        data: { title: 'Team', description: 'Manage organization team members, roles, and invitations.' },
        title: 'Team – GateFlow'
      },
      {
        path: 'settings',
        component: PlaceholderComponent,
        data: { title: 'Settings', description: 'Configure organization details, domains, and security policies.' },
        title: 'Settings – GateFlow'
      }
    ]
  },
  {
    path: '**',
    redirectTo: 'overview'
  }
];
