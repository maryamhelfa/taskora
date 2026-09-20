import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';

import { Project } from '../../core/models/project.model';
import { Team, TeamMember, TeamRole } from '../../core/models/team.model';
import { ProjectService } from '../../core/services/project.service';
import { TeamService } from '../../core/services/team.service';
import { TopNav } from '../../shared/top-nav/top-nav';

@Component({
  selector: 'app-teams',
  imports: [ReactiveFormsModule, RouterLink, TopNav],
  templateUrl: './teams.html',
  styleUrl: './teams.css',
})
export class Teams implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly teamService = inject(TeamService);
  private readonly projectService = inject(ProjectService);

  readonly teams = signal<Team[]>([]);
  readonly loadingTeams = signal(false);
  readonly teamsError = signal<string | null>(null);

  readonly selectedTeamId = signal<number | null>(null);

  readonly members = signal<TeamMember[]>([]);
  readonly loadingMembers = signal(false);
  readonly membersError = signal<string | null>(null);

  readonly projects = signal<Project[]>([]);
  readonly loadingProjects = signal(false);
  readonly projectsError = signal<string | null>(null);

  readonly creatingTeam = signal(false);
  readonly teamForm = this.fb.nonNullable.group({
    name: ['', Validators.required],
  });

  readonly addingMember = signal(false);
  readonly memberError = signal<string | null>(null);
  readonly memberForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    role: ['MEMBER' as TeamRole],
  });

  readonly savingProject = signal(false);
  readonly projectError = signal<string | null>(null);
  readonly editingProjectId = signal<number | null>(null);
  readonly projectForm = this.fb.nonNullable.group({
    name: ['', Validators.required],
    description: [''],
  });

  ngOnInit(): void {
    this.loadTeams();
  }

  loadTeams(): void {
    this.loadingTeams.set(true);
    this.teamsError.set(null);

    this.teamService.listTeams().subscribe({
      next: (teams) => {
        this.loadingTeams.set(false);
        this.teams.set(teams);
        if (this.selectedTeamId() === null && teams.length > 0) {
          this.selectTeam(teams[0].id);
        }
      },
      error: (error: HttpErrorResponse) => {
        this.loadingTeams.set(false);
        this.teamsError.set(error.error?.message ?? 'Impossible de charger les equipes.');
      },
    });
  }

  submitTeamForm(): void {
    if (this.teamForm.invalid) {
      this.teamForm.markAllAsTouched();
      return;
    }

    this.creatingTeam.set(true);
    this.teamsError.set(null);

    this.teamService.createTeam(this.teamForm.getRawValue()).subscribe({
      next: (team) => {
        this.creatingTeam.set(false);
        this.teamForm.reset({ name: '' });
        this.teams.update((current) => [...current, team]);
        this.selectTeam(team.id);
      },
      error: (error: HttpErrorResponse) => {
        this.creatingTeam.set(false);
        this.teamsError.set(error.error?.message ?? "Impossible de creer l'equipe.");
      },
    });
  }

  selectTeam(teamId: number): void {
    this.selectedTeamId.set(teamId);
    this.cancelEditProject();
    this.loadMembers(teamId);
    this.loadProjects(teamId);
  }

  loadMembers(teamId: number): void {
    this.loadingMembers.set(true);
    this.membersError.set(null);

    this.teamService.listMembers(teamId).subscribe({
      next: (members) => {
        this.loadingMembers.set(false);
        this.members.set(members);
      },
      error: (error: HttpErrorResponse) => {
        this.loadingMembers.set(false);
        this.membersError.set(error.error?.message ?? 'Impossible de charger les membres.');
      },
    });
  }

  submitMemberForm(): void {
    const teamId = this.selectedTeamId();
    if (teamId === null || this.memberForm.invalid) {
      this.memberForm.markAllAsTouched();
      return;
    }

    this.addingMember.set(true);
    this.memberError.set(null);

    this.teamService.addMember(teamId, this.memberForm.getRawValue()).subscribe({
      next: (member) => {
        this.addingMember.set(false);
        this.memberForm.reset({ email: '', role: 'MEMBER' });
        this.members.update((current) => [...current, member]);
      },
      error: (error: HttpErrorResponse) => {
        this.addingMember.set(false);
        this.memberError.set(error.error?.message ?? "Impossible d'ajouter ce membre.");
      },
    });
  }

  loadProjects(teamId: number): void {
    this.loadingProjects.set(true);
    this.projectsError.set(null);

    this.projectService.listProjects(teamId).subscribe({
      next: (projects) => {
        this.loadingProjects.set(false);
        this.projects.set(projects);
      },
      error: (error: HttpErrorResponse) => {
        this.loadingProjects.set(false);
        this.projectsError.set(error.error?.message ?? 'Impossible de charger les projets.');
      },
    });
  }

  startCreateProject(): void {
    this.editingProjectId.set(0);
    this.projectError.set(null);
    this.projectForm.reset({ name: '', description: '' });
  }

  startEditProject(project: Project): void {
    this.editingProjectId.set(project.id);
    this.projectError.set(null);
    this.projectForm.reset({ name: project.name, description: project.description ?? '' });
  }

  cancelEditProject(): void {
    this.editingProjectId.set(null);
    this.projectError.set(null);
  }

  submitProjectForm(): void {
    const teamId = this.selectedTeamId();
    const editingId = this.editingProjectId();
    if (teamId === null || editingId === null || this.projectForm.invalid) {
      this.projectForm.markAllAsTouched();
      return;
    }

    const { name, description } = this.projectForm.getRawValue();
    this.savingProject.set(true);
    this.projectError.set(null);

    if (editingId === 0) {
      this.projectService.createProject({ teamId, name, description }).subscribe({
        next: (project) => {
          this.savingProject.set(false);
          this.projects.update((current) => [...current, project]);
          this.cancelEditProject();
        },
        error: (error: HttpErrorResponse) => {
          this.savingProject.set(false);
          this.projectError.set(error.error?.message ?? 'Impossible de creer le projet.');
        },
      });
      return;
    }

    const existing = this.projects().find((p) => p.id === editingId);
    if (!existing) {
      return;
    }

    this.projectService.updateProject(editingId, { name, description, status: existing.status }).subscribe({
      next: (project) => {
        this.savingProject.set(false);
        this.projects.update((current) => current.map((p) => (p.id === project.id ? project : p)));
        this.cancelEditProject();
      },
      error: (error: HttpErrorResponse) => {
        this.savingProject.set(false);
        this.projectError.set(error.error?.message ?? 'Impossible de modifier le projet.');
      },
    });
  }

  toggleArchive(project: Project): void {
    const newStatus = project.status === 'ACTIVE' ? 'ARCHIVED' : 'ACTIVE';
    this.projectsError.set(null);

    this.projectService
      .updateProject(project.id, { name: project.name, description: project.description, status: newStatus })
      .subscribe({
        next: (updated) => {
          this.projects.update((current) => current.map((p) => (p.id === updated.id ? updated : p)));
        },
        error: (error: HttpErrorResponse) => {
          this.projectsError.set(error.error?.message ?? 'Impossible de modifier le statut du projet.');
        },
      });
  }

  deleteProject(project: Project): void {
    if (!confirm(`Supprimer le projet "${project.name}" ? Cette action est irreversible.`)) {
      return;
    }

    this.projectsError.set(null);

    this.projectService.deleteProject(project.id).subscribe({
      next: () => {
        this.projects.update((current) => current.filter((p) => p.id !== project.id));
      },
      error: (error: HttpErrorResponse) => {
        this.projectsError.set(error.error?.message ?? 'Impossible de supprimer le projet.');
      },
    });
  }
}
