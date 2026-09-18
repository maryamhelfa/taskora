package com.taskora.backend.ai;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.anthropic.client.AnthropicClient;
import com.anthropic.errors.AnthropicIoException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.InternalServerException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.TextBlock;
import com.taskora.backend.ai.dto.AiSummaryResponse;
import com.taskora.backend.exception.AiServiceException;
import com.taskora.backend.exception.ResourceNotFoundException;
import com.taskora.backend.project.Project;
import com.taskora.backend.project.ProjectRepository;
import com.taskora.backend.task.Task;
import com.taskora.backend.task.TaskRepository;
import com.taskora.backend.team.TeamAccessService;
import com.taskora.backend.user.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiSummaryService {

    private static final long MAX_TOKENS = 1024L;

    private final AnthropicClient anthropicClient;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final TeamAccessService teamAccessService;

    @Value("${app.ai.model}")
    private String model;

    public AiSummaryResponse generateSummary(User currentUser, Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable : " + projectId));
        teamAccessService.requireMembership(project.getTeam().getId(), currentUser.getId());

        List<Task> tasks = taskRepository.findAllByProjectId(projectId);
        String prompt = buildPrompt(project, tasks);
        String summary = callClaude(prompt);

        return new AiSummaryResponse(summary, Instant.now());
    }

    private String callClaude(String prompt) {
        MessageCreateParams params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(MAX_TOKENS)
                .addUserMessage(prompt)
                .build();

        try {
            Message response = anthropicClient.messages().create(params);
            String text = response.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(TextBlock::text)
                    .collect(Collectors.joining("\n"))
                    .strip();

            if (text.isEmpty()) {
                throw new AiServiceException("Le service IA n'a renvoye aucun contenu. Merci de reessayer.");
            }
            return text;
        } catch (RateLimitException e) {
            throw new AiServiceException("Le quota de l'API IA est depasse pour le moment. Merci de reessayer plus tard.");
        } catch (InternalServerException e) {
            throw new AiServiceException("Le service IA est momentanement surcharge. Merci de reessayer dans quelques instants.");
        } catch (AnthropicIoException e) {
            throw new AiServiceException("Le service IA n'a pas repondu a temps (timeout). Merci de reessayer.");
        } catch (AnthropicServiceException e) {
            throw new AiServiceException("Le service IA a renvoye une erreur inattendue. Merci de reessayer plus tard.");
        }
    }

    private String buildPrompt(Project project, List<Task> tasks) {
        StringBuilder sb = new StringBuilder();
        sb.append("Voici l'etat actuel du projet ").append(project.getName()).append(".\n");
        sb.append("Date du jour : ").append(LocalDate.now()).append("\n\n");

        if (tasks.isEmpty()) {
            sb.append("Aucune tache n'est encore enregistree pour ce projet.\n");
        } else {
            sb.append("Taches :\n");
            for (Task task : tasks) {
                sb.append("- ").append(task.getTitle())
                        .append(" | statut : ").append(task.getStatus())
                        .append(" | priorite : ").append(task.getPriority())
                        .append(" | echeance : ").append(task.getDueDate() != null ? task.getDueDate() : "non definie")
                        .append(" | assigne : ").append(formatAssignee(task))
                        .append("\n");
            }
        }

        sb.append("\nGenere un resume en francais, en 5 a 8 lignes, structure en trois parties : ")
                .append("ce qui avance bien, ce qui est en retard ou a risque, et une recommandation de priorite ")
                .append("pour les jours a venir. Ton professionnel et synthetique, pas de jargon inutile. ")
                .append("Reponds en texte brut uniquement, sans mise en forme Markdown (pas de titres avec #, ")
                .append("pas de gras avec **, pas de listes a puces).");

        return sb.toString();
    }

    private String formatAssignee(Task task) {
        User assignee = task.getAssignee();
        if (assignee == null) {
            return "non assigne";
        }
        return assignee.getFirstName() + " " + assignee.getLastName();
    }

}
