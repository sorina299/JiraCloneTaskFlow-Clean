package com.sorina.taskflow.controller;

import com.sorina.taskflow.dto.IssueCreateDTO;
import com.sorina.taskflow.dto.IssueDTO;
import com.sorina.taskflow.dto.IssueUpdateDTO;
import com.sorina.taskflow.enums.IssuePriority;
import com.sorina.taskflow.enums.IssueStatus;
import com.sorina.taskflow.enums.IssueType;
import com.sorina.taskflow.service.IssueService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping
public class IssueController {

    private final IssueService issueService;

    public IssueController(IssueService issueService) {
        this.issueService = issueService;
    }

    // POST /projects/{projectId}/issues  -> create
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/projects/{projectId}/issues")
    public ResponseEntity<IssueDTO> createIssue(
            @PathVariable UUID projectId,
            @RequestBody IssueCreateDTO dto
    ) {
        return ResponseEntity.ok(issueService.createIssue(projectId, dto));
    }

    // GET /projects/{projectId}/issues -> list
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/projects/{projectId}/issues")
    public ResponseEntity<List<IssueDTO>> getIssuesByProject(
            @PathVariable UUID projectId,
            @RequestParam(required = false) IssueStatus status,
            @RequestParam(required = false) IssueType type,
            @RequestParam(required = false) String assigneeName,
            @RequestParam(required = false) IssuePriority priority
    ) {
        return ResponseEntity.ok(
                issueService.getIssuesByProject(projectId, status, type, assigneeName, priority)
        );
    }

    // GET /issues/{id} -> details
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/issues/{id}")
    public ResponseEntity<IssueDTO> getIssue(@PathVariable UUID id) {
        return ResponseEntity.ok(issueService.getIssueById(id));
    }

    // PATCH /issues/{id} -> partial update
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/issues/{id}")
    public ResponseEntity<IssueDTO> patchIssue(
            @PathVariable UUID id,
            @RequestBody IssueUpdateDTO dto
    ) {
        return ResponseEntity.ok(issueService.partialUpdateIssue(id, dto));
    }

    // DELETE /issues/{id}
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/issues/{id}")
    public ResponseEntity<String> deleteIssue(@PathVariable UUID id) {
        issueService.deleteIssue(id);
        return ResponseEntity.ok("Issue deleted successfully");
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/issues/{id}/assign-to-me")
    public ResponseEntity<IssueDTO> assignToMe(@PathVariable UUID id) {
        return ResponseEntity.ok(issueService.assignIssueToCurrentUser(id));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/issues/{id}/status")
    public ResponseEntity<IssueDTO> changeStatus(
            @PathVariable UUID id,
            @RequestParam IssueStatus status
    ) {
        return ResponseEntity.ok(issueService.changeIssueStatus(id, status));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/issues/{id}/reopen")
    public ResponseEntity<IssueDTO> reopenIssue(@PathVariable UUID id) {
        return ResponseEntity.ok(issueService.reopenIssue(id));
    }


    @PreAuthorize("isAuthenticated()")
    @PostMapping("/issues/{id}/unassign")
    public ResponseEntity<IssueDTO> unassignIssue(@PathVariable UUID id) {
        return ResponseEntity.ok(issueService.unassignIssue(id));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/issues/{parentId}/subtasks")
    public ResponseEntity<IssueDTO> createSubtask(
            @PathVariable UUID parentId,
            @RequestBody IssueCreateDTO dto
    ) {
        return ResponseEntity.ok(issueService.createSubtask(parentId, dto));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/issues/{parentId}/subtasks")
    public ResponseEntity<List<IssueDTO>> getSubtasks(@PathVariable UUID parentId) {
        return ResponseEntity.ok(issueService.getSubtasks(parentId));
    }
}