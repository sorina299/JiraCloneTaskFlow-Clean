package com.sorina.taskflow.repository;

import com.sorina.taskflow.entity.Project;
import com.sorina.taskflow.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
    Optional<Project> findByKey(String key);
    List<Project> findAllByOwner(User owner);
    List<Project> findAllByMembersContaining(User member);
}
