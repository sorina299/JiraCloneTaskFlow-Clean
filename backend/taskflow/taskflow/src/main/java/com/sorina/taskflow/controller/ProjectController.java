package com.sorina.taskflow.controller;

import com.sorina.taskflow.dto.ProjectDTO;
import com.sorina.taskflow.dto.ProjectInvitationDTO;
import com.sorina.taskflow.entity.ProjectInvitation;
import com.sorina.taskflow.enums.ProjectRole;
import com.sorina.taskflow.service.ProjectService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<ProjectDTO> createProject(@RequestBody ProjectDTO dto) {
        return ResponseEntity.ok(projectService.createProject(dto));
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}")
    public ResponseEntity<ProjectDTO> updateProject(
            @PathVariable UUID id,
            @RequestBody ProjectDTO projectDto
    ) {
        return ResponseEntity.ok(projectService.updateProject(id, projectDto));
    }

    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/{id}")
    public ResponseEntity<ProjectDTO> patchProject(
            @PathVariable UUID id,
            @RequestBody ProjectDTO dto
    ) {
        return ResponseEntity.ok(projectService.partialUpdateProject(id, dto));
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteProject(@PathVariable UUID id) {
        projectService.deleteProject(id);
        return ResponseEntity.ok("Project deleted successfully");
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<List<ProjectDTO>> getUserProjects() {
        return ResponseEntity.ok(projectService.getUserProjects());
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<ProjectDTO> getProjectById(@PathVariable UUID id) {
        return ResponseEntity.ok(projectService.getProjectById(id));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{projectId}/invite")
    public ResponseEntity<String> inviteUser(
            @PathVariable UUID projectId,
            @RequestParam String identifier,    // username or email
            @RequestParam(defaultValue = "DEVELOPER") ProjectRole role
    ) {
        projectService.inviteUserToProject(projectId, identifier, role);
        return ResponseEntity.ok("Invitation sent");
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/invitations")
    public ResponseEntity<List<ProjectInvitationDTO>> getMyInvitations() {
        return ResponseEntity.ok(projectService.getMyPendingInvitations());
    }

    // Accept invitation
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/invitations/{invitationId}/accept")
    public ResponseEntity<String> acceptInvitation(@PathVariable UUID invitationId) {
        projectService.acceptInvitation(invitationId);
        return ResponseEntity.ok("Invitation accepted");
    }

    // Decline invitation
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/invitations/{invitationId}/decline")
    public ResponseEntity<String> declineInvitation(@PathVariable UUID invitationId) {
        projectService.declineInvitation(invitationId);
        return ResponseEntity.ok("Invitation declined");
    }
}
