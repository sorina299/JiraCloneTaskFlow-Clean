package com.sorina.taskflow.repository;

import com.sorina.taskflow.entity.Issue;
import com.sorina.taskflow.enums.IssuePriority;
import com.sorina.taskflow.enums.IssueStatus;
import com.sorina.taskflow.enums.IssueType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface IssueRepository extends JpaRepository<Issue, UUID> {

    List<Issue> findAllByProjectId(UUID projectId);

    @Query("""
        SELECT i FROM Issue i
        WHERE i.project.id = :projectId
          AND (:status IS NULL OR i.status = :status)
          AND (:type IS NULL OR i.type = :type)
          AND (:priority IS NULL OR i.priority = :priority)
          AND (:filterByAssignee = false OR i.assignee.id IN :assigneeIds)
        """)
    List<Issue> searchByProjectAndFilters(
            @Param("projectId") UUID projectId,
            @Param("status") IssueStatus status,
            @Param("type") IssueType type,
            @Param("priority") IssuePriority priority,
            @Param("filterByAssignee") boolean filterByAssignee,
            @Param("assigneeIds") List<UUID> assigneeIds
    );

    List<Issue> findAllByParentIssueId(UUID parentId);
}
