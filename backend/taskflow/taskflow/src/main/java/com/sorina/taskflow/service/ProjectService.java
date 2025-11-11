package com.sorina.taskflow.service;

import com.sorina.taskflow.dto.ProjectDTO;
import com.sorina.taskflow.dto.ProjectInvitationDTO;
import com.sorina.taskflow.dto.ProjectMemberDTO;
import com.sorina.taskflow.entity.Project;
import com.sorina.taskflow.entity.ProjectInvitation;
import com.sorina.taskflow.entity.ProjectMember;
import com.sorina.taskflow.entity.User;
import com.sorina.taskflow.enums.InvitationStatus;
import com.sorina.taskflow.enums.ProjectRole;
import com.sorina.taskflow.repository.ProjectInvitationRepository;
import com.sorina.taskflow.repository.ProjectMemberRepository;
import com.sorina.taskflow.repository.ProjectRepository;
import com.sorina.taskflow.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectInvitationRepository invitationRepository;
    private final EmailService emailService;

    public ProjectService(ProjectRepository projectRepository,
                          UserRepository userRepository,
                          ProjectMemberRepository projectMemberRepository,
                          ProjectInvitationRepository invitationRepository,
                          EmailService emailService) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.invitationRepository = invitationRepository;
        this.emailService = emailService;
    }

    public ProjectDTO createProject(ProjectDTO dto) {
        User currentUser = getCurrentUser();

        Project project = new Project();
        project.setKey(dto.key());
        project.setName(dto.name());
        project.setDescription(dto.description());
        project.setOwner(currentUser);

        project.addMember(currentUser, ProjectRole.PROJECT_MANAGER);

        Project saved = projectRepository.save(project);
        return toDTO(saved);
    }

    public ProjectDTO updateProject(UUID id, ProjectDTO dto) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        User current = getCurrentUser();
        if (!hasProjectRole(project, current, ProjectRole.PROJECT_MANAGER)) {
            throw new RuntimeException("Not authorized to update this project");
        }

        project.setKey(dto.key());
        project.setName(dto.name());
        project.setDescription(dto.description());

        Project updated = projectRepository.save(project);
        return toDTO(updated);
    }

    public void deleteProject(UUID id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        User current = getCurrentUser();
        if (!hasProjectRole(project, current, ProjectRole.PROJECT_MANAGER)) {
            throw new RuntimeException("Not authorized to delete this project");
        }

        projectRepository.delete(project);
    }

    @Transactional
    public List<ProjectDTO> getUserProjects() {
        User currentUser = getCurrentUser();

        List<Project> owned = projectRepository.findAllByOwner(currentUser);
        List<Project> memberProjects = projectMemberRepository.findAllByUser(currentUser)
                .stream()
                .map(ProjectMember::getProject)
                .toList();

        return java.util.stream.Stream.concat(owned.stream(), memberProjects.stream())
                .distinct()
                .map(this::toDTO)
                .toList();
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public ProjectDTO getProjectById(UUID id) {
        User currentUser = getCurrentUser();

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        boolean isMember = projectMemberRepository.existsByProjectAndUser(project, currentUser);

        if (!isMember && !project.getOwner().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Not authorized to view this project");
        }

        return toDTO(project);
    }


    private ProjectDTO toDTO(Project project) {
        var memberDTOs = project.getMembers()
                .stream()
                .map(pm -> new ProjectMemberDTO(
                        pm.getUser().getId(),
                        pm.getUser().getUsername(),
                        pm.getUser().getEmail(),
                        pm.getUser().getFirstName(),
                        pm.getUser().getLastName(),
                        pm.getRole()
                ))
                .toList();

        return new ProjectDTO(
                project.getId(),
                project.getKey(),
                project.getName(),
                project.getDescription(),
                memberDTOs
        );
    }

    public ProjectDTO partialUpdateProject(UUID id, ProjectDTO dto) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (!project.getOwner().getId().equals(getCurrentUser().getId())) {
            throw new RuntimeException("Not authorized to update this project");
        }

        if (dto.key() != null && !dto.key().isBlank()) {
            project.setKey(dto.key());
        }
        if (dto.name() != null && !dto.name().isBlank()) {
            project.setName(dto.name());
        }
        if (dto.description() != null && !dto.description().isBlank()) {
            project.setDescription(dto.description());
        }

        Project updated = projectRepository.save(project);
        return toDTO(updated);
    }

    private boolean hasProjectRole(Project project, User user, ProjectRole... allowedRoles) {
        if (project.getOwner().getId().equals(user.getId())) {
            return true; // owner is always treated as manager
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

    public void inviteUserToProject(UUID projectId, String identifier, ProjectRole role) {
        User current = getCurrentUser();

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (!hasProjectRole(project, current, ProjectRole.PROJECT_MANAGER)) {
            throw new RuntimeException("Not authorized to invite users to this project");
        }

        User invited = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (projectMemberRepository.existsByProjectAndUser(project, invited)) {
            throw new RuntimeException("User is already a project member");
        }

        ProjectInvitation invitation = new ProjectInvitation();
        invitation.setProject(project);
        invitation.setInvitedUser(invited);
        invitation.setRole(role);

        ProjectInvitation saved = invitationRepository.save(invitation);

        // ✅ send real email
        emailService.sendProjectInvitationEmail(saved);
    }

    public void acceptInvitation(UUID invitationId) {
        User current = getCurrentUser();

        ProjectInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new RuntimeException("Invitation not found"));

        if (!invitation.getInvitedUser().getId().equals(current.getId())) {
            throw new RuntimeException("You are not the invited user for this invitation");
        }

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new RuntimeException("Invitation is not pending");
        }

        // add as member with the specified role
        Project project = invitation.getProject();
        if (!projectMemberRepository.existsByProjectAndUser(project, current)) {
            project.addMember(current, invitation.getRole());
            projectRepository.save(project);
        }

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitationRepository.save(invitation);
    }

    public void declineInvitation(UUID invitationId) {
        User current = getCurrentUser();

        ProjectInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new RuntimeException("Invitation not found"));

        if (!invitation.getInvitedUser().getId().equals(current.getId())) {
            throw new RuntimeException("You are not the invited user for this invitation");
        }

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new RuntimeException("Invitation is not pending");
        }

        invitation.setStatus(InvitationStatus.DECLINED);
        invitationRepository.save(invitation);
    }

    public List<ProjectInvitationDTO> getMyPendingInvitations() {
        User current = getCurrentUser();

        return invitationRepository
                .findAllByInvitedUserAndStatus(current, InvitationStatus.PENDING)
                .stream()
                .map(inv -> new ProjectInvitationDTO(
                        inv.getId(),
                        inv.getProject().getId(),
                        inv.getProject().getKey(),
                        inv.getProject().getName(),
                        inv.getRole(),
                        inv.getStatus(),
                        inv.getCreatedAt()
                ))
                .toList();
    }
}