package com.sorina.taskflow.service;

import com.sorina.taskflow.entity.ProjectInvitation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from}")
    private String from;

    // Backend URL base (for testing without UI)
    @Value("${app.backend.base-url:http://localhost:8080}")
    private String backendBaseUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendProjectInvitationEmail(ProjectInvitation invitation) {
        String to = invitation.getInvitedUser().getEmail();

        String subject = "You’ve been invited to project " + invitation.getProject().getName();

        // Build backend links for accepting/declining
        String acceptLink = backendBaseUrl + "/projects/invitations/" + invitation.getId() + "/accept";
        String declineLink = backendBaseUrl + "/projects/invitations/" + invitation.getId() + "/decline";

        String text = """
                Hi %s,

                You’ve been invited to join the project "%s" as %s.

                To respond to this invitation, use one of the links below:

                ✅ Accept Invitation:
                %s

                ❌ Decline Invitation:
                %s

                If you didn’t expect this email, you can ignore it.

                — TaskFlow
                """.formatted(
                invitation.getInvitedUser().getFirstName() != null
                        ? invitation.getInvitedUser().getFirstName()
                        : invitation.getInvitedUser().getUsername(),
                invitation.getProject().getName(),
                invitation.getRole(),
                acceptLink,
                declineLink
        );

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);

        mailSender.send(message);
    }
}