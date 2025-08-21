package com.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class EmailVerificationService {

    private final Map<UUID, VerificationSession> verificationSessions = new ConcurrentHashMap<>();

    public static class VerificationSession {
        private final String requestedEmail;
        private final String cui;
        private final Integer years;
        private final LocalDateTime expiresAt;
        private boolean verified = false;
        private String verifiedEmail;

        public VerificationSession(String requestedEmail, String cui, Integer years) {
            this.requestedEmail = requestedEmail;
            this.cui = cui;
            this.years = years;
            this.expiresAt = LocalDateTime.now().plusMinutes(10);
        }

        public String getRequestedEmail() { return requestedEmail; }
        public String getCui() { return cui; }
        public Integer getYears() { return years; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
        public boolean isVerified() { return verified; }
        public void setVerified(boolean verified) { this.verified = verified; }
        public String getVerifiedEmail() { return verifiedEmail; }
        public void setVerifiedEmail(String verifiedEmail) { this.verifiedEmail = verifiedEmail; }

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }

        public boolean isEmailMatching() {
            return requestedEmail == null || (verifiedEmail != null && verifiedEmail.equalsIgnoreCase(requestedEmail));
        }
    }

    public String initiateEmailVerification(String email, String cui, Integer years) {
        cleanupExpiredSessions();

        UUID sessionId = UUID.randomUUID();
        VerificationSession session = new VerificationSession(email, cui, years);
        verificationSessions.put(sessionId, session);

        log.info("Created email verification session {} for CUI: {} (requestedEmail provided)",
                sessionId, cui);

        return sessionId.toString();
    }

    public String initiateEmailVerification(String cui, Integer years) {
        cleanupExpiredSessions();
        UUID sessionId = UUID.randomUUID();
        VerificationSession session = new VerificationSession(null, cui, years);
        verificationSessions.put(sessionId, session);
        log.info("Created email verification session {} for CUI: {} (no requestedEmail)", sessionId, cui);
        return sessionId.toString();
    }

    public boolean verifyGoogleAuth(UUID sessionId, String googleEmail) {
        VerificationSession session = verificationSessions.get(sessionId);

        if (session == null) {
            log.warn("Session not found: {}", sessionId);
            return false;
        }

        if (session.isExpired()) {
            log.warn("Session expired: {}", sessionId);
            verificationSessions.remove(sessionId);
            return false;
        }

        session.setVerifiedEmail(googleEmail);
        session.setVerified(true);

        boolean emailMatches = session.isEmailMatching();
        log.info("Email verification for session {}: requested={}, verified={}, matches={}",
                sessionId, session.getRequestedEmail(), googleEmail, emailMatches);

        return emailMatches;
    }

    public VerificationSession getVerifiedSession(UUID sessionId) {
        VerificationSession session = verificationSessions.get(sessionId);

        if (session == null || session.isExpired() || !session.isVerified() || !session.isEmailMatching()) {
            return null;
        }

        return session;
    }

    public boolean isSessionVerified(String sessionId) {
        try {
            UUID uuid = UUID.fromString(sessionId);
            VerificationSession session = verificationSessions.get(uuid);
            return session != null && !session.isExpired() && session.isVerified() && session.isEmailMatching();
        } catch (IllegalArgumentException e) {
            log.warn("Invalid session ID format: {}", sessionId);
            return false;
        }
    }

    public VerificationSession getSession(String sessionId) {
        try {
            UUID uuid = UUID.fromString(sessionId);
            VerificationSession session = verificationSessions.get(uuid);

            if (session != null && session.isExpired()) {
                verificationSessions.remove(uuid);
                return null;
            }

            return session;
        } catch (IllegalArgumentException e) {
            log.warn("Invalid session ID format: {}", sessionId);
            return null;
        }
    }

    public void cleanupSession(String sessionId) {
        try {
            UUID uuid = UUID.fromString(sessionId);
            verificationSessions.remove(uuid);
            log.info("Cleaned up session: {}", sessionId);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid session ID format for cleanup: {}", sessionId);
        }
    }


    public void completeSession(UUID sessionId) {
        verificationSessions.remove(sessionId);
        log.info("Completed and removed session: {}", sessionId);
    }

    private String generateGoogleOAuthUrl(UUID sessionId) {
        return "http://localhost:8080/oauth2/authorization/google?state=" + sessionId;
    }

    private void cleanupExpiredSessions() {
        verificationSessions.entrySet().removeIf(entry -> {
            boolean expired = entry.getValue().isExpired();
            if (expired) {
                log.debug("Removing expired session: {}", entry.getKey());
            }
            return expired;
        });
    }
    public int getActiveSessionsCount() {
        cleanupExpiredSessions();
        return verificationSessions.size();
    }
}
