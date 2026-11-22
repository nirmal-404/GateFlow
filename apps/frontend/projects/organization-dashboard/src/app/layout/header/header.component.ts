import { Component, inject } from '@angular/core';
import { Router, NavigationEnd, RouterLink } from '@angular/router';
import { filter } from 'rxjs/operators';
import { MatMenuModule } from '@angular/material/menu';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { NavigationService } from '../../core/services/navigation.service';

@Component({
  selector: 'app-header',
  imports: [RouterLink, MatMenuModule, MatButtonModule, MatTooltipModule],
  templateUrl: './header.component.html',
  styleUrl: './header.component.css'
})
export class HeaderComponent {
  readonly navService = inject(NavigationService);
  private readonly router = inject(Router);

  currentPageTitle = 'Overview';

  constructor() {
    this.updateTitle(this.router.url);
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event: any) => {
      this.updateTitle(event.urlAfterRedirects || event.url);
    });
  }

  private updateTitle(url: string): void {
    const segment = url.split('/')[1]?.split('?')[0] || 'overview';
    const found = this.navService.navItems.find(item => item.route.includes(segment));
    this.currentPageTitle = found ? found.label : 'Overview';
  }
}
