package com.sorina.taskflow.filter;

import com.sorina.taskflow.repository.TokenRepository;
import com.sorina.taskflow.service.JwtService;
import com.sorina.taskflow.service.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwt;
    private final UserDetailsServiceImpl uds;
    private final TokenRepository tokenRepository;

    public JwtAuthenticationFilter(JwtService jwt,
                                   UserDetailsServiceImpl uds,
                                   TokenRepository tokenRepository) {
        this.jwt = jwt;
        this.uds = uds;
        this.tokenRepository = tokenRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws IOException, ServletException {

        String path = request.getServletPath();

        // ✅ Skip swagger and api-docs requests completely
        if (path.contains("/v3/api-docs") ||
                path.contains("/swagger-ui") ||
                path.contains("/swagger-resources") ||
                path.contains("/webjars")) {
            chain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        String username;
        try {
            username = jwt.extractUsername(token);
        } catch (Exception e) {
            chain.doFilter(request, response);
            return;
        }

        var storedToken = tokenRepository.findByAccessToken(token).orElse(null);
        if (storedToken == null || storedToken.isLoggedOut()) {
            chain.doFilter(request, response);
            return;
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            var user = uds.loadUserByUsername(username);
            if (jwt.isAccessTokenValid(token, user)) {
                var auth = new UsernamePasswordAuthenticationToken(
                        user, null, user.getAuthorities()
                );
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        chain.doFilter(request, response);
    }
}