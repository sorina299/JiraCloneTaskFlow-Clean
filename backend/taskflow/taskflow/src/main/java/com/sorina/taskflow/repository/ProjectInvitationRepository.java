package com.sorina.taskflow.repository;

import com.sorina.taskflow.entity.ProjectInvitation;
import com.sorina.taskflow.entity.User;
import com.sorina.taskflow.enums.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectInvitationRepository extends JpaRepository<ProjectInvitation, UUID> {

    List<ProjectInvitation> findAllByInvitedUserAndStatus(User user, InvitationStatus status);
}
