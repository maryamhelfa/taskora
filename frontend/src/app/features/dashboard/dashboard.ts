import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';
import { DashboardService } from '../../core/services/dashboard.service';
import { Dashboard as DashboardData } from '../../core/models/dashboard.model';
import { TopNav } from '../../shared/top-nav/top-nav';

@Component({
  selector: 'app-dashboard',
  imports: [TopNav, RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly dashboardService = inject(DashboardService);

  readonly currentUser = this.authService.currentUser;

  readonly dashboard = signal<DashboardData | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  readonly totalTodo = computed(
    () => this.dashboard()?.projects.reduce((sum, p) => sum + p.todoCount, 0) ?? 0,
  );
  readonly totalInProgress = computed(
    () => this.dashboard()?.projects.reduce((sum, p) => sum + p.inProgressCount, 0) ?? 0,
  );
  readonly totalDone = computed(
    () => this.dashboard()?.projects.reduce((sum, p) => sum + p.doneCount, 0) ?? 0,
  );

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);

    this.dashboardService.getDashboard().subscribe({
      next: (dashboard) => {
        this.loading.set(false);
        this.dashboard.set(dashboard);
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(error.error?.message ?? 'Impossible de charger le tableau de bord.');
      },
    });
  }
}
