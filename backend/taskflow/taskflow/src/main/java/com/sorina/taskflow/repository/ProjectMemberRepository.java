package com.sorina.taskflow.repository;

import com.sorina.taskflow.entity.Project;
import com.sorina.taskflow.entity.ProjectMember;
import com.sorina.taskflow.entity.User;
import com.sorina.taskflow.enums.ProjectRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {

    Optional<ProjectMember> findByProjectAndUser(Project project, User user);

    List<ProjectMember> findAllByUser(User user);

    List<ProjectMember> findAllByProject(Project project);

    boolean existsByProjectAndUser(Project project, User user);

    boolean existsByProjectAndUserAndRole(Project project, User user, ProjectRole role);
}
