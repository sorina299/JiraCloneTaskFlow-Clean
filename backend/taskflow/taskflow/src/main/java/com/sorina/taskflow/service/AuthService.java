package com.sorina.taskflow.service;

import com.sorina.taskflow.dto.*;
import com.sorina.taskflow.entity.Role;
import com.sorina.taskflow.entity.Token;
import com.sorina.taskflow.entity.User;
import com.sorina.taskflow.enums.RoleType;
import com.sorina.taskflow.repository.RoleRepository;
import com.sorina.taskflow.repository.TokenRepository;
import com.sorina.taskflow.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authManager;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository, TokenRepository tokenRepository,
                       PasswordEncoder passwordEncoder, JwtService jwtService, AuthenticationManager authManager) {
        this.userRepository = userRepository; this.roleRepository = roleRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder; this.jwtService = jwtService; this.authManager = authManager;
    }

    public RegisterResponseDTO register(RegisterRequestDTO req) {
        if (userRepository.existsByUsername(req.username())) throw new RuntimeException("Username already taken");
        if (userRepository.existsByEmail(req.email())) throw new RuntimeException("Email already registered");

        Role userRole = roleRepository.findByName(RoleType.USER).orElseThrow(() -> new RuntimeException("Role USER missing"));

        User user = new User();
        user.setUsername(req.username());
        user.setEmail(req.email());
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setFirstName(req.firstName());
        user.setLastName(req.lastName());
        user.setRoles(Set.of(userRole));
        var saved = userRepository.save(user);

        var userDetails = org.springframework.security.core.userdetails.User
                .withUsername(saved.getUsername())
                .password(saved.getPassword())
                .authorities(saved.getRoles().stream().map(role -> "ROLE_" + role.getName()).toArray(String[]::new))
                .build();

        String access = jwtService.generateAccessToken(userDetails);
        String refresh = jwtService.generateRefreshToken(userDetails);

        Token token = new Token();
        token.setAccessToken(access);
        token.setRefreshToken(refresh);
        token.setLoggedOut(false);
        token.setUser(saved);
        tokenRepository.save(token);

        var userDTO = new UserResponseDTO(
                saved.getId().toString(),
                saved.getUsername(),
                saved.getEmail(),
                saved.getFirstName(),
                saved.getLastName(),
                saved.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()),
                saved.getProfilePictureUrl()
        );


        var tokenDTO = new AuthenticationResponseDTO(access, refresh, 900);

        return new RegisterResponseDTO(tokenDTO, userDTO);
    }

    public AuthenticationResponseDTO login(LoginRequestDTO req) {
        authManager.authenticate(new UsernamePasswordAuthenticationToken(req.username(), req.password()));
        User user = userRepository.findByUsername(req.username()).orElseThrow();

        UserDetails ud = org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername()).password(user.getPassword())
                .authorities(user.getRoles().stream().map(role->"ROLE_"+role.getName()).toArray(String[]::new)).build();

        // revoke previous tokens
        tokenRepository.findAllByUserAndLoggedOutFalse(user).forEach(token -> token.setLoggedOut(true));
        tokenRepository.saveAll(tokenRepository.findAllByUserAndLoggedOutFalse(user));

        String access  = jwtService.generateAccessToken(ud);
        String refresh = jwtService.generateRefreshToken(ud);

        Token token = new Token();
        token.setAccessToken(access);
        token.setRefreshToken(refresh);
        token.setLoggedOut(false);
        token.setUser(user);
        tokenRepository.save(token);

        return new AuthenticationResponseDTO(access, refresh, 900);
    }

    public AuthenticationResponseDTO refresh(RefreshTokenRequestDTO req) {
        String token = req.refreshToken();
        String username = jwtService.extractUsername(token);
        User u = userRepository.findByUsername(username).orElseThrow();
        if (!jwtService.isRefreshTokenValid(token, u)) throw new RuntimeException("Invalid refresh token");

        UserDetails ud = org.springframework.security.core.userdetails.User
                .withUsername(u.getUsername()).password(u.getPassword())
                .authorities(u.getRoles().stream().map(r->"ROLE_"+r.getName()).toArray(String[]::new)).build();

        tokenRepository.findAllByUserAndLoggedOutFalse(u).forEach(tk -> tk.setLoggedOut(true));
        tokenRepository.saveAll(tokenRepository.findAllByUserAndLoggedOutFalse(u));

        String newAccess = jwtService.generateAccessToken(ud);
        String newRefresh = jwtService.generateRefreshToken(ud);
        Token t = new Token();
        t.setAccessToken(newAccess);
        t.setRefreshToken(newRefresh);
        t.setUser(u);
        tokenRepository.save(t);

        return new AuthenticationResponseDTO(newAccess, newRefresh, 900);
    }

    public void logout(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);

        tokenRepository.findByAccessToken(token).ifPresent(t -> {
            t.setLoggedOut(true);
            tokenRepository.save(t);
        });

        tokenRepository.findByRefreshToken(token).ifPresent(t -> {
            t.setLoggedOut(true);
            tokenRepository.save(t);
        });
    }

}
