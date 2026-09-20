import { Component, OnInit, WritableSignal, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import {
  CdkDragDrop,
  DragDropModule,
  moveItemInArray,
  transferArrayItem,
} from '@angular/cdk/drag-drop';

import { Project } from '../../../core/models/project.model';
import { Task, TaskPriority, TaskStatus } from '../../../core/models/task.model';
import { TeamMember } from '../../../core/models/team.model';
import { ProjectService } from '../../../core/services/project.service';
import { TaskService } from '../../../core/services/task.service';
import { TeamService } from '../../../core/services/team.service';
import { TopNav } from '../../../shared/top-nav/top-nav';
import { TaskDetailModal } from '../task-detail-modal/task-detail-modal';

const COLUMN_STATUS: Record<string, TaskStatus> = {
  'column-todo': 'TODO',
  'column-in-progress': 'IN_PROGRESS',
  'column-done': 'DONE',
};

const PRIORITY_LABELS: Record<TaskPriority, string> = {
  LOW: 'Basse',
  MEDIUM: 'Moyenne',
  HIGH: 'Haute',
};

@Component({
  selector: 'app-project-detail',
  imports: [ReactiveFormsModule, DragDropModule, DatePipe, TopNav, TaskDetailModal],
  templateUrl: './project-detail.html',
  styleUrl: './project-detail.css',
})
export class ProjectDetail implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly projectService = inject(ProjectService);
  private readonly taskService = inject(TaskService);
  private readonly teamService = inject(TeamService);

  private readonly projectId = Number(this.route.snapshot.paramMap.get('id'));

  readonly project = signal<Project | null>(null);
  readonly loadingProject = signal(false);
  readonly projectError = signal<string | null>(null);

  readonly members = signal<TeamMember[]>([]);

  readonly todoTasks = signal<Task[]>([]);
  readonly inProgressTasks = signal<Task[]>([]);
  readonly doneTasks = signal<Task[]>([]);
  readonly loadingTasks = signal(false);
  readonly tasksError = signal<string | null>(null);
  readonly boardError = signal<string | null>(null);

  readonly selectedTask = signal<Task | null>(null);

  readonly showTaskForm = signal(false);
  readonly creatingTask = signal(false);
  readonly taskFormError = signal<string | null>(null);
  readonly taskForm = this.fb.nonNullable.group({
    title: ['', Validators.required],
    priority: ['MEDIUM' as TaskPriority],
    dueDate: [''],
    assigneeId: [''],
  });

  ngOnInit(): void {
    this.loadProject();
    this.loadTasks();
  }

  priorityLabel(priority: TaskPriority): string {
    return PRIORITY_LABELS[priority];
  }

  loadProject(): void {
    this.loadingProject.set(true);
    this.projectError.set(null);

    this.projectService.listProjects().subscribe({
      next: (projects) => {
        this.loadingProject.set(false);
        const project = projects.find((p) => p.id === this.projectId) ?? null;
        this.project.set(project);
        if (project) {
          this.loadMembers(project.teamId);
        } else {
          this.projectError.set('Projet introuvable.');
        }
      },
      error: (error: HttpErrorResponse) => {
        this.loadingProject.set(false);
        this.projectError.set(error.error?.message ?? 'Impossible de charger le projet.');
      },
    });
  }

  loadMembers(teamId: number): void {
    this.teamService.listMembers(teamId).subscribe({
      next: (members) => this.members.set(members),
    });
  }

  loadTasks(): void {
    this.loadingTasks.set(true);
    this.tasksError.set(null);

    this.taskService.listTasksForProject(this.projectId).subscribe({
      next: (tasks) => {
        this.loadingTasks.set(false);
        this.todoTasks.set(tasks.filter((t) => t.status === 'TODO'));
        this.inProgressTasks.set(tasks.filter((t) => t.status === 'IN_PROGRESS'));
        this.doneTasks.set(tasks.filter((t) => t.status === 'DONE'));
      },
      error: (error: HttpErrorResponse) => {
        this.loadingTasks.set(false);
        this.tasksError.set(error.error?.message ?? 'Impossible de charger les taches.');
      },
    });
  }

  private columnFor(status: TaskStatus): WritableSignal<Task[]> {
    if (status === 'TODO') return this.todoTasks;
    if (status === 'IN_PROGRESS') return this.inProgressTasks;
    return this.doneTasks;
  }

  drop(event: CdkDragDrop<Task[]>): void {
    const previousStatus = COLUMN_STATUS[event.previousContainer.id];
    const newStatus = COLUMN_STATUS[event.container.id];
    const previousColumn = this.columnFor(previousStatus);
    const currentColumn = this.columnFor(newStatus);

    if (event.previousContainer === event.container) {
      const items = [...currentColumn()];
      moveItemInArray(items, event.previousIndex, event.currentIndex);
      currentColumn.set(items);
      return;
    }

    const previousItems = [...previousColumn()];
    const currentItems = [...currentColumn()];
    transferArrayItem(previousItems, currentItems, event.previousIndex, event.currentIndex);
    previousColumn.set(previousItems);
    currentColumn.set(currentItems);

    const task = currentItems[event.currentIndex];
    const originIndex = event.previousIndex;
    task.status = newStatus;
    this.boardError.set(null);

    this.taskService.updateStatus(task.id, newStatus).subscribe({
      error: (error: HttpErrorResponse) => {
        task.status = previousStatus;
        const revertCurrent = [...currentColumn()];
        const revertPrevious = [...previousColumn()];
        const idx = revertCurrent.indexOf(task);
        if (idx !== -1) {
          transferArrayItem(revertCurrent, revertPrevious, idx, originIndex);
        }
        currentColumn.set(revertCurrent);
        previousColumn.set(revertPrevious);
        this.boardError.set(error.error?.message ?? 'Impossible de deplacer la tache.');
      },
    });
  }

  openTask(task: Task): void {
    this.selectedTask.set(task);
  }

  closeTask(): void {
    this.selectedTask.set(null);
  }

  toggleTaskForm(): void {
    this.showTaskForm.update((v) => !v);
    this.taskFormError.set(null);
    if (this.showTaskForm()) {
      this.taskForm.reset({ title: '', priority: 'MEDIUM', dueDate: '', assigneeId: '' });
    }
  }

  submitTaskForm(): void {
    if (this.taskForm.invalid) {
      this.taskForm.markAllAsTouched();
      return;
    }

    const { title, priority, dueDate, assigneeId } = this.taskForm.getRawValue();
    this.creatingTask.set(true);
    this.taskFormError.set(null);

    this.taskService
      .createTask({
        projectId: this.projectId,
        title,
        priority,
        dueDate: dueDate || null,
        assigneeId: assigneeId ? Number(assigneeId) : null,
      })
      .subscribe({
        next: (task) => {
          this.creatingTask.set(false);
          this.todoTasks.update((current) => [...current, task]);
          this.showTaskForm.set(false);
        },
        error: (error: HttpErrorResponse) => {
          this.creatingTask.set(false);
          this.taskFormError.set(error.error?.message ?? 'Impossible de creer la tache.');
        },
      });
  }
}
