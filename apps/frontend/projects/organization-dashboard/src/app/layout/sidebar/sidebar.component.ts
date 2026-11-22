import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { MatMenuModule } from '@angular/material/menu';
import { MatButtonModule } from '@angular/material/button';
import { NavigationService, Organization } from '../../core/services/navigation.service';

@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, RouterLinkActive, MatMenuModule, MatButtonModule],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css'
})
export class SidebarComponent {
  readonly navService = inject(NavigationService);
  readonly currentOrg = this.navService.currentOrganization;
  readonly availableOrgs = this.navService.availableOrganizations;
  readonly navItems = this.navService.navItems;

  switchOrg(org: Organization): void {
    this.navService.selectOrganization(org);
  }

  onNavigate(): void {
    this.navService.closeMobileSidebar();
  }
}
