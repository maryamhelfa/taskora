import { Component, effect, inject, input, output, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { Subtask, Task, TaskComment, TaskPriority } from '../../../core/models/task.model';
import { TaskService } from '../../../core/services/task.service';

const PRIORITY_LABELS: Record<TaskPriority, string> = {
  LOW: 'Basse',
  MEDIUM: 'Moyenne',
  HIGH: 'Haute',
};

@Component({
  selector: 'app-task-detail-modal',
  imports: [FormsModule, DatePipe],
  templateUrl: './task-detail-modal.html',
  styleUrl: './task-detail-modal.css',
})
export class TaskDetailModal {
  private readonly taskService = inject(TaskService);

  readonly task = input.required<Task>();
  readonly closed = output<void>();
  readonly commentAdded = output<void>();

  readonly subtasks = signal<Subtask[]>([]);
  readonly comments = signal<TaskComment[]>([]);

  readonly newSubtaskTitle = signal('');
  readonly addingSubtask = signal(false);
  readonly subtasksError = signal<string | null>(null);

  readonly newComment = signal('');
  readonly addingComment = signal(false);
  readonly commentsError = signal<string | null>(null);

  constructor() {
    effect(() => {
      const task = this.task();
      this.subtasks.set(task.subtasks);
      this.comments.set(task.comments);
      this.newSubtaskTitle.set('');
      this.subtasksError.set(null);
      this.newComment.set('');
      this.commentsError.set(null);
    });
  }

  priorityLabel(priority: TaskPriority): string {
    return PRIORITY_LABELS[priority];
  }

  assigneeName(): string {
    const task = this.task();
    if (!task.assigneeId) {
      return 'Non assignee';
    }
    return `${task.assigneeFirstName} ${task.assigneeLastName}`;
  }

  close(): void {
    this.closed.emit();
  }

  toggleSubtask(subtask: Subtask): void {
    const done = !subtask.done;
    this.subtasksError.set(null);
    this.subtasks.update((current) =>
      current.map((s) => (s.id === subtask.id ? { ...s, done } : s)),
    );

    this.taskService.updateSubtask(subtask.id, { done }).subscribe({
      error: (error: HttpErrorResponse) => {
        this.subtasks.update((current) =>
          current.map((s) => (s.id === subtask.id ? { ...s, done: !done } : s)),
        );
        this.subtasksError.set(error.error?.message ?? 'Impossible de mettre a jour la sous-tache.');
      },
    });
  }

  addSubtask(): void {
    const title = this.newSubtaskTitle().trim();
    if (!title) {
      return;
    }

    this.addingSubtask.set(true);
    this.subtasksError.set(null);

    this.taskService.addSubtask(this.task().id, title).subscribe({
      next: (subtask) => {
        this.addingSubtask.set(false);
        this.subtasks.update((current) => [...current, subtask]);
        this.newSubtaskTitle.set('');
      },
      error: (error: HttpErrorResponse) => {
        this.addingSubtask.set(false);
        this.subtasksError.set(error.error?.message ?? 'Impossible d\'ajouter la sous-tache.');
      },
    });
  }

  addComment(): void {
    const content = this.newComment().trim();
    if (!content) {
      return;
    }

    this.addingComment.set(true);
    this.commentsError.set(null);

    this.taskService.addComment(this.task().id, content).subscribe({
      next: (comment) => {
        this.addingComment.set(false);
        this.comments.update((current) => [...current, comment]);
        this.newComment.set('');
        this.commentAdded.emit();
      },
      error: (error: HttpErrorResponse) => {
        this.addingComment.set(false);
        this.commentsError.set(error.error?.message ?? 'Impossible d\'ajouter le commentaire.');
      },
    });
  }
}
