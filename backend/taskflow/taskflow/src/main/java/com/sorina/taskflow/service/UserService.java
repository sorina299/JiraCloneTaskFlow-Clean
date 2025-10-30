package com.sorina.taskflow.service;

import com.sorina.taskflow.dto.AccountSettingsDTO;
import com.sorina.taskflow.dto.RegisterRequestDTO;
import com.sorina.taskflow.dto.UserProfileDTO;
import com.sorina.taskflow.entity.Role;
import com.sorina.taskflow.entity.User;
import com.sorina.taskflow.enums.RoleType;
import com.sorina.taskflow.repository.RoleRepository;
import com.sorina.taskflow.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User createUser(RegisterRequestDTO req, String roleName) {
        if (userRepository.existsByUsername(req.username())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(req.email())) {
            throw new RuntimeException("Email already exists");
        }

        RoleType roleType;
        try {
            roleType = RoleType.valueOf(roleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role name: " + roleName);
        }

        Role role = roleRepository.findByName(roleType)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));

        User user = new User();
        user.setUsername(req.username());
        user.setEmail(req.email());
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setFirstName(req.firstName());
        user.setLastName(req.lastName());
        user.setEnabled(true);
        user.setRoles(Set.of(role)); // assign single role initially

        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User updateUserRole(UUID userId, String roleName) {
        User user = getUserById(userId);

        RoleType roleType;
        try {
            roleType = RoleType.valueOf(roleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role name: " + roleName);
        }

        Role role = roleRepository.findByName(roleType)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));

        user.getRoles().clear();
        user.getRoles().add(role);
        return userRepository.save(user);
    }


    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found");
        }
        userRepository.deleteById(id);
    }

    public User getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User updateProfile(UserProfileDTO dto) {
        User user = getCurrentUser();

        if (dto.firstName() != null) user.setFirstName(dto.firstName());
        if (dto.lastName() != null) user.setLastName(dto.lastName());
        if (dto.pronouns() != null) user.setPronouns(dto.pronouns());
        if (dto.jobTitle() != null) user.setJobTitle(dto.jobTitle());
        if (dto.description() != null) user.setDescription(dto.description());
        if (dto.profilePictureUrl() != null) user.setProfilePictureUrl(dto.profilePictureUrl());

        return userRepository.save(user);
    }

    public void changePassword(AccountSettingsDTO dto) {
        User user = getCurrentUser();

        if (!passwordEncoder.matches(dto.currentPassword(), user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        if (!dto.newPassword().equals(dto.confirmPassword())) {
            throw new RuntimeException("New password and confirmation do not match");
        }

        user.setPassword(passwordEncoder.encode(dto.newPassword()));
        userRepository.save(user);
    }

    public void deleteOwnAccount(String currentPassword) {
        User user = getCurrentUser();

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Password verification failed");
        }

        userRepository.delete(user);
    }

    public User updateProfilePicture(String imageUrl) {
        User user = getCurrentUser();
        user.setProfilePictureUrl(imageUrl);
        return userRepository.save(user);
    }
}