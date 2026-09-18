import { Component, inject } from '@angular/core';

import { AuthService } from '../../core/services/auth.service';
import { TopNav } from '../../shared/top-nav/top-nav';

@Component({
  selector: 'app-dashboard',
  imports: [TopNav],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard {
  private readonly authService = inject(AuthService);

  readonly currentUser = this.authService.currentUser;
}
