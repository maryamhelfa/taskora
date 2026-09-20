import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { CreateTaskRequest, Task, TaskStatus } from '../models/task.model';

@Injectable({ providedIn: 'root' })
export class TaskService {
  private readonly http = inject(HttpClient);
  private readonly tasksUrl = `${environment.apiBaseUrl}/tasks`;
  private readonly projectsUrl = `${environment.apiBaseUrl}/projects`;

  listTasksForProject(projectId: number): Observable<Task[]> {
    return this.http.get<Task[]>(`${this.projectsUrl}/${projectId}/tasks`);
  }

  createTask(request: CreateTaskRequest): Observable<Task> {
    return this.http.post<Task>(this.tasksUrl, request);
  }

  updateStatus(id: number, status: TaskStatus): Observable<Task> {
    return this.http.patch<Task>(`${this.tasksUrl}/${id}/status`, { status });
  }
}
