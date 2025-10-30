package com.sorina.taskflow.controller;

import com.sorina.taskflow.dto.*;
import com.sorina.taskflow.entity.Role;
import com.sorina.taskflow.entity.User;
import com.sorina.taskflow.enums.RoleType;
import com.sorina.taskflow.service.GoogleDriveService;
import com.sorina.taskflow.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final GoogleDriveService googleDriveService;

    public UserController(UserService userService, GoogleDriveService googleDriveService) {
        this.userService = userService;
        this.googleDriveService = googleDriveService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody RegisterRequestDTO req,
                                           @RequestParam String role) {
        return ResponseEntity.ok(userService.createUser(req, role));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> getUsers() {
        return ResponseEntity.ok(userService.getAllUsersAsDTO());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/role")
    public ResponseEntity<User> updateRole(@PathVariable UUID id, @RequestParam String role) {
        return ResponseEntity.ok(userService.updateUserRole(id, role));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.ok("User deleted successfully");
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getCurrentUser() {
        return ResponseEntity.ok(userService.getCurrentUserResponse());
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/me/profile")
    public ResponseEntity<UserResponseDTO> updateProfile(@RequestBody UserProfileDTO dto) {
        return ResponseEntity.ok(userService.updateProfile(dto));
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/me/password")
    public ResponseEntity<String> changePassword(@Valid @RequestBody AccountSettingsDTO dto) {
        userService.changePassword(dto);
        return ResponseEntity.ok("Password changed successfully");
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/me")
    public ResponseEntity<String> deleteOwnAccount(@RequestParam String password) {
        userService.deleteOwnAccount(password);
        return ResponseEntity.ok("Account deleted successfully");
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = "/me/avatar", consumes = "multipart/form-data")
    @Operation(summary = "Upload avatar", description = "Uploads user profile picture")
    public ResponseEntity<UserResponseDTO> uploadProfilePicture(@RequestParam("image") MultipartFile file)
            throws IOException, GeneralSecurityException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // Upload the image to Google Drive
        File tempFile = File.createTempFile("profile_", null);
        file.transferTo(tempFile);
        UploadResponseDTO res = googleDriveService.uploadImageToDrive(tempFile);

        // Update user profile picture in DB
        User updatedUser = userService.updateProfilePicture(res.url());

        // Map to UserResponseDTO
        Set<RoleType> roles = updatedUser.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toSet());

        UserResponseDTO response = new UserResponseDTO(
                updatedUser.getId().toString(),
                updatedUser.getUsername(),
                updatedUser.getEmail(),
                updatedUser.getFirstName(),
                updatedUser.getLastName(),
                roles,
                updatedUser.getProfilePictureUrl()
        );

        return ResponseEntity.ok(response);
    }
}
