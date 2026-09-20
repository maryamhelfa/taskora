import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AiSummaryResponse, CreateProjectRequest, Project, UpdateProjectRequest } from '../models/project.model';

@Injectable({ providedIn: 'root' })
export class ProjectService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/projects`;

  listProjects(teamId?: number): Observable<Project[]> {
    const params = teamId != null ? new HttpParams().set('teamId', teamId) : undefined;
    return this.http.get<Project[]>(this.baseUrl, { params });
  }

  createProject(request: CreateProjectRequest): Observable<Project> {
    return this.http.post<Project>(this.baseUrl, request);
  }

  updateProject(id: number, request: UpdateProjectRequest): Observable<Project> {
    return this.http.put<Project>(`${this.baseUrl}/${id}`, request);
  }

  deleteProject(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  generateAiSummary(id: number): Observable<AiSummaryResponse> {
    return this.http.post<AiSummaryResponse>(`${this.baseUrl}/${id}/ai-summary`, {});
  }
}
