package com.sorina.taskflow.repository;

import com.sorina.taskflow.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("""
        SELECT u FROM User u
        WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :name, '%'))
           OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :name, '%'))
           OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :name, '%'))
           OR LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :name, '%'))
        """)
    List<User> searchByName(@Param("name") String name);
}
