package com.sorina.taskflow.service;

import com.sorina.taskflow.dto.IssueCreateDTO;
import com.sorina.taskflow.dto.IssueDTO;
import com.sorina.taskflow.dto.IssueUpdateDTO;
import com.sorina.taskflow.entity.Issue;
import com.sorina.taskflow.entity.Project;
import com.sorina.taskflow.entity.User;
import com.sorina.taskflow.enums.IssuePriority;
import com.sorina.taskflow.enums.IssueStatus;
import com.sorina.taskflow.enums.IssueType;
import com.sorina.taskflow.enums.ProjectRole;
import com.sorina.taskflow.repository.IssueRepository;
import com.sorina.taskflow.repository.ProjectMemberRepository;
import com.sorina.taskflow.repository.ProjectRepository;
import com.sorina.taskflow.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class IssueService {

    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;

    public IssueService(IssueRepository issueRepository,
                        ProjectRepository projectRepository,
                        ProjectMemberRepository projectMemberRepository,
                        UserRepository userRepository) {
        this.issueRepository = issueRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.userRepository = userRepository;
    }

    // ---------- helper methods ----------

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private boolean hasProjectRole(Project project, User user, ProjectRole... allowedRoles) {
        if (project.getOwner().getId().equals(user.getId())) {
            return true; // owner always allowed
        }

        return projectMemberRepository.findByProjectAndUser(project, user)
                .filter(pm -> {
                    for (ProjectRole role : allowedRoles) {
                        if (pm.getRole() == role) return true;
                    }
                    return false;
                })
                .isPresent();
    }

    private boolean isProjectMemberOrOwner(Project project, User user) {
        return project.getOwner().getId().equals(user.getId())
                || projectMemberRepository.existsByProjectAndUser(project, user);
    }

    private IssueDTO toDTO(Issue issue) {
        return new IssueDTO(
                issue.getId(),
                issue.getTitle(),
                issue.getDescription(),
                issue.getType(),
                issue.getStatus(),
                issue.getPriority(),
                issue.getProject().getId(),
                issue.getReporter().getId(),
                issue.getAssignee() != null ? issue.getAssignee().getId() : null,
                issue.getParentIssue() != null ? issue.getParentIssue().getId() : null,
                issue.getCreatedAt(),
                issue.getUpdatedAt()
        );
    }

    private boolean isTransitionAllowed(IssueStatus from, IssueStatus to) {
        if (from == to) {
            return true; // no-op, always allowed
        }

        return switch (from) {
            case TO_DO -> (to == IssueStatus.IN_PROGRESS);
            case IN_PROGRESS -> (to == IssueStatus.IN_REVIEW || to == IssueStatus.TO_DO);
            case IN_REVIEW -> (to == IssueStatus.DONE || to == IssueStatus.IN_PROGRESS);
            case DONE -> false; // handled separately via reopen
        };
    }


    // ---------- CREATE ----------

    @Transactional
    public IssueDTO createIssue(UUID projectId, IssueCreateDTO dto) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        User current = getCurrentUser();

        if (!isProjectMemberOrOwner(project, current)) {
            throw new RuntimeException("Not authorized to create issues in this project");
        }

        // 🔐 SUBTASK + parent consistency
        if (dto.type() == IssueType.SUBTASK && dto.parentIssueId() == null) {
            throw new RuntimeException("Subtasks must have a parent issue");
        }
        if (dto.parentIssueId() != null && dto.type() != IssueType.SUBTASK) {
            throw new RuntimeException("Only subtasks can have a parent issue");
        }

        Issue issue = new Issue();
        issue.setTitle(dto.title());
        issue.setDescription(dto.description());
        issue.setType(dto.type());
        issue.setPriority(dto.priority());
        issue.setStatus(dto.status() != null ? dto.status() : IssueStatus.TO_DO);
        issue.setProject(project);
        issue.setReporter(current);

        if (dto.assigneeId() != null) {
            User assignee = userRepository.findById(dto.assigneeId())
                    .orElseThrow(() -> new RuntimeException("Assignee not found"));

            if (!isProjectMemberOrOwner(project, assignee)) {
                throw new RuntimeException("Assignee is not a member of this project");
            }

            issue.setAssignee(assignee);
        }

        if (dto.parentIssueId() != null) {
            Issue parent = issueRepository.findById(dto.parentIssueId())
                    .orElseThrow(() -> new RuntimeException("Parent issue not found"));

            if (!parent.getProject().getId().equals(project.getId())) {
                throw new RuntimeException("Parent issue belongs to a different project");
            }

            if (parent.getType() == IssueType.SUBTASK) {
                throw new RuntimeException("Subtasks cannot have their own subtasks");
            }

            issue.setParentIssue(parent);
        }

        Issue saved = issueRepository.save(issue);
        return toDTO(saved);
    }

    // ---------- READ ----------

    public IssueDTO getIssueById(UUID id) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Issue not found"));

        User current = getCurrentUser();
        if (!isProjectMemberOrOwner(issue.getProject(), current)) {
            throw new RuntimeException("Not authorized to view this issue");
        }

        return toDTO(issue);
    }

    @Transactional
    public List<IssueDTO> getIssuesByProject(
            UUID projectId,
            IssueStatus status,
            IssueType type,
            String assigneeName,
            IssuePriority priority
    ) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        User current = getCurrentUser();
        if (!isProjectMemberOrOwner(project, current)) {
            throw new RuntimeException("Not authorized to view issues for this project");
        }

        // 🔍 handle assigneeName -> list of user IDs
        boolean filterByAssignee = false;
        List<UUID> assigneeIds = List.of();

        if (assigneeName != null && !assigneeName.isBlank()) {
            List<User> matchingUsers = userRepository.searchByName(assigneeName);

            if (matchingUsers.isEmpty()) {
                return List.of();
            }

            assigneeIds = matchingUsers.stream()
                    .map(User::getId)
                    .toList();
            filterByAssignee = true;
        }


        return issueRepository
                .searchByProjectAndFilters(projectId, status, type, priority, filterByAssignee, assigneeIds)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    // ---------- UPDATE (PATCH) ----------

    @Transactional
    public IssueDTO partialUpdateIssue(UUID id, IssueUpdateDTO dto) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Issue not found"));

        User current = getCurrentUser();
        Project project = issue.getProject();

        boolean isProjectManager = hasProjectRole(project, current, ProjectRole.PROJECT_MANAGER);
        boolean isDeveloper = !isProjectManager && hasProjectRole(project, current, ProjectRole.DEVELOPER);

        if (!isProjectManager && !isDeveloper) {
            throw new RuntimeException("Not authorized to update this issue");
        }

        if (isDeveloper) {
            boolean isReporter = issue.getReporter() != null
                    && issue.getReporter().getId().equals(current.getId());
            boolean isAssignee = issue.getAssignee() != null
                    && issue.getAssignee().getId().equals(current.getId());

            if (!isReporter && !isAssignee) {
                throw new RuntimeException("Developers can only update issues they reported or are assigned to");
            }
        }

        if (dto.title() != null && !dto.title().isBlank()) {
            issue.setTitle(dto.title());
        }
        if (dto.description() != null && !dto.description().isBlank()) {
            issue.setDescription(dto.description());
        }
        if (dto.type() != null) {
            issue.setType(dto.type());
        }
        if (dto.status() != null) {
            issue.setStatus(dto.status());
        }
        if (dto.priority() != null) {
            issue.setPriority(dto.priority());
        }
        if (dto.assigneeId() != null) {
            User assignee = userRepository.findById(dto.assigneeId())
                    .orElseThrow(() -> new RuntimeException("Assignee not found"));

            if (!isProjectMemberOrOwner(issue.getProject(), assignee)) {
                throw new RuntimeException("Assignee is not a member of this project");
            }

            issue.setAssignee(assignee);
        }
        if (dto.parentIssueId() != null) {
            Issue parent = issueRepository.findById(dto.parentIssueId())
                    .orElseThrow(() -> new RuntimeException("Parent issue not found"));

            if (!parent.getProject().getId().equals(issue.getProject().getId())) {
                throw new RuntimeException("Parent issue belongs to a different project");
            }

            issue.setParentIssue(parent);
        }

        Issue updated = issueRepository.save(issue);
        return toDTO(updated);
    }

    // ---------- DELETE ----------

    @Transactional
    public void deleteIssue(UUID id) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Issue not found"));

        User current = getCurrentUser();
        Project project = issue.getProject();

        boolean isProjectManager = hasProjectRole(project, current, ProjectRole.PROJECT_MANAGER);

        boolean isReporter = issue.getReporter() != null &&
                issue.getReporter().getId().equals(current.getId());

        // If the user is not PM AND not the reporter → deny
        if (!isProjectManager && !isReporter) {
            throw new RuntimeException(
                    "You are not authorized to delete this issue. Only project managers or the issue reporter may delete it."
            );
        }

        issueRepository.delete(issue);
    }

    @Transactional
    public IssueDTO assignIssueToCurrentUser(UUID issueId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found"));

        User current = getCurrentUser();
        Project project = issue.getProject();

        // Must be at least member of the project
        if (!isProjectMemberOrOwner(project, current)) {
            throw new RuntimeException("Not authorized to assign this issue");
        }

        // Only PM or DEVELOPER can assign
        if (!hasProjectRole(project, current,
                ProjectRole.PROJECT_MANAGER, ProjectRole.DEVELOPER)) {
            throw new RuntimeException("Not allowed to assign issues in this project");
        }

        issue.setAssignee(current);

        Issue updated = issueRepository.save(issue);
        return toDTO(updated);
    }

    @Transactional
    public IssueDTO changeIssueStatus(UUID issueId, IssueStatus newStatus) {
        if (newStatus == null) {
            throw new RuntimeException("Status must not be null");
        }

        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found"));

        User current = getCurrentUser();
        Project project = issue.getProject();

        boolean isProjectManager = hasProjectRole(project, current, ProjectRole.PROJECT_MANAGER);
        boolean isDeveloper = !isProjectManager && hasProjectRole(project, current, ProjectRole.DEVELOPER);

        // Viewer or non-member → no
        if (!isProjectManager && !isDeveloper) {
            throw new RuntimeException("Not authorized to change status for this issue");
        }

        // Developers can only change status for issues assigned to them
        if (isDeveloper) {
            boolean isAssignee = issue.getAssignee() != null
                    && issue.getAssignee().getId().equals(current.getId());

            if (!isAssignee) {
                throw new RuntimeException(
                        "Developers can only change status for issues assigned to them"
                );
            }
        }

        IssueStatus currentStatus = issue.getStatus();

        // DONE is "locked": must use reopen endpoint to leave DONE
        if (currentStatus == IssueStatus.DONE && newStatus != IssueStatus.DONE) {
            throw new RuntimeException(
                    "Issues in DONE can only be reopened using the reopen action"
            );
        }

        // ✅ NEW: when moving to DONE, all subtasks must be DONE
        if (newStatus == IssueStatus.DONE && !issue.getSubtasks().isEmpty()) {
            boolean allSubtasksDone = issue.getSubtasks().stream()
                    .allMatch(sub -> sub.getStatus() == IssueStatus.DONE);

            if (!allSubtasksDone) {
                throw new RuntimeException(
                        "Cannot move parent issue to DONE while some subtasks are not DONE"
                );
            }
        }

        // Validate normal transitions
        if (!isTransitionAllowed(currentStatus, newStatus)) {
            throw new RuntimeException(
                    "Invalid status transition: " + currentStatus + " → " + newStatus
            );
        }

        issue.setStatus(newStatus);

        Issue updated = issueRepository.save(issue);
        return toDTO(updated);
    }

    @Transactional
    public IssueDTO reopenIssue(UUID issueId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found"));

        User current = getCurrentUser();
        Project project = issue.getProject();

        boolean isProjectManager = hasProjectRole(project, current, ProjectRole.PROJECT_MANAGER);
        boolean isDeveloper = !isProjectManager && hasProjectRole(project, current, ProjectRole.DEVELOPER);

        if (!isProjectManager && !isDeveloper) {
            throw new RuntimeException("Not authorized to reopen this issue");
        }

        // Developers: only if they are assignee
        if (isDeveloper) {
            boolean isAssignee = issue.getAssignee() != null
                    && issue.getAssignee().getId().equals(current.getId());

            if (!isAssignee) {
                throw new RuntimeException(
                        "Developers can only reopen issues assigned to them"
                );
            }
        }

        if (issue.getStatus() != IssueStatus.DONE) {
            throw new RuntimeException("Only issues in DONE can be reopened");
        }

        issue.setStatus(IssueStatus.IN_PROGRESS);

        Issue updated = issueRepository.save(issue);
        return toDTO(updated);
    }

    @Transactional
    public IssueDTO unassignIssue(UUID issueId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found"));

        User current = getCurrentUser();
        Project project = issue.getProject();

        boolean isProjectManager = hasProjectRole(project, current, ProjectRole.PROJECT_MANAGER);
        boolean isDeveloper = !isProjectManager && hasProjectRole(project, current, ProjectRole.DEVELOPER);

        // VIEWER or non-member → not allowed
        if (!isProjectManager && !isDeveloper) {
            throw new RuntimeException("Not authorized to unassign this issue");
        }

        // Developers can only unassign issues they reported OR are assigned to
        if (isDeveloper) {
            boolean isReporter = issue.getReporter() != null
                    && issue.getReporter().getId().equals(current.getId());
            boolean isAssignee = issue.getAssignee() != null
                    && issue.getAssignee().getId().equals(current.getId());

            if (!isReporter && !isAssignee) {
                throw new RuntimeException("Developers can only unassign issues they reported or are assigned to");
            }
        }

        // If already unassigned, do nothing gracefully
        issue.setAssignee(null);

        Issue updated = issueRepository.save(issue);
        return toDTO(updated);
    }

    @Transactional
    public IssueDTO createSubtask(UUID parentId, IssueCreateDTO dto) {
        Issue parent = issueRepository.findById(parentId)
                .orElseThrow(() -> new RuntimeException("Parent issue not found"));

        Project project = parent.getProject();
        User current = getCurrentUser();

        if (!isProjectMemberOrOwner(project, current)) {
            throw new RuntimeException("Not authorized to create subtasks in this project");
        }

        if (parent.getType() == IssueType.SUBTASK) {
            throw new RuntimeException("Subtasks cannot have their own subtasks");
        }

        Issue subtask = new Issue();
        subtask.setTitle(dto.title());
        subtask.setDescription(dto.description());
        subtask.setType(IssueType.SUBTASK); // 🔒 always SUBTASK here
        subtask.setPriority(dto.priority());
        subtask.setStatus(dto.status() != null ? dto.status() : IssueStatus.TO_DO);
        subtask.setProject(project);
        subtask.setReporter(current);
        subtask.setParentIssue(parent);

        if (dto.assigneeId() != null) {
            User assignee = userRepository.findById(dto.assigneeId())
                    .orElseThrow(() -> new RuntimeException("Assignee not found"));

            if (!isProjectMemberOrOwner(project, assignee)) {
                throw new RuntimeException("Assignee is not a member of this project");
            }

            subtask.setAssignee(assignee);
        }

        Issue saved = issueRepository.save(subtask);
        return toDTO(saved);
    }

    @Transactional
    public List<IssueDTO> getSubtasks(UUID parentId) {
        Issue parent = issueRepository.findById(parentId)
                .orElseThrow(() -> new RuntimeException("Parent issue not found"));

        User current = getCurrentUser();
        if (!isProjectMemberOrOwner(parent.getProject(), current)) {
            throw new RuntimeException("Not authorized to view subtasks for this issue");
        }

        return issueRepository.findAllByParentIssueId(parentId)
                .stream()
                .map(this::toDTO)
                .toList();
    }
}
