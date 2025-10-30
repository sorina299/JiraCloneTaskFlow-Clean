package com.sorina.taskflow.service;

import com.sorina.taskflow.dto.ProjectDTO;
import com.sorina.taskflow.entity.Project;
import com.sorina.taskflow.entity.User;
import com.sorina.taskflow.repository.ProjectRepository;
import com.sorina.taskflow.repository.UserRepository;
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

    public ProjectService(ProjectRepository projectRepository, UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    public ProjectDTO createProject(ProjectDTO dto) {
        User currentUser = getCurrentUser();

        Project project = new Project();
        project.setKey(dto.key());
        project.setName(dto.name());
        project.setDescription(dto.description());
        project.setOwner(currentUser);
        project.getMembers().add(currentUser);

        Project saved = projectRepository.save(project);
        return toDTO(saved);
    }

    public ProjectDTO updateProject(UUID id, ProjectDTO dto) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (!project.getOwner().getId().equals(getCurrentUser().getId())) {
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
        if (!project.getOwner().getId().equals(getCurrentUser().getId())) {
            throw new RuntimeException("Not authorized to delete this project");
        }
        projectRepository.delete(project);
    }

    public List<ProjectDTO> getUserProjects() {
        User currentUser = getCurrentUser();
        List<Project> owned = projectRepository.findAllByOwner(currentUser);
        List<Project> member = projectRepository.findAllByMembersContaining(currentUser);

        return owned.stream()
                .distinct()
                .collect(Collectors.toList())
                .stream()
                .map(this::toDTO)
                .toList();
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public ProjectDTO getProjectById(UUID id) {
        User currentUser = getCurrentUser();

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (!project.getOwner().getId().equals(currentUser.getId()) &&
                !project.getMembers().contains(currentUser)) {
            throw new RuntimeException("Not authorized to view this project");
        }

        return toDTO(project);
    }

    private ProjectDTO toDTO(Project project) {
        return new ProjectDTO(project.getId(), project.getKey(), project.getName(), project.getDescription());
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
}