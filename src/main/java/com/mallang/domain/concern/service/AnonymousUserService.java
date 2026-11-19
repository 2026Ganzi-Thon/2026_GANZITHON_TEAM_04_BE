package com.mallang.domain.concern.service;

import com.mallang.common.exception.GeneralException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Opaque bearer credentials. The database stores only a SHA-256 digest. */
@Service
public class AnonymousUserService {
    private final SecureRandom random = new SecureRandom();
    private final JdbcTemplate jdbc;

    public AnonymousUserService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Registration register() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String userId = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO app_user (user_id, token_hash, created_at, step_count) VALUES (?, ?, ?, 0)",
                userId, digest(token), java.sql.Timestamp.from(Instant.now()));
        return new Registration(userId, token);
    }

    public String requireUser(String authorization) {
        if (authorization == null || !authorization.matches("Bearer [A-Za-z0-9_-]{43}")) {
            throw new GeneralException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }
        String token = authorization.substring(7);
        String userId = jdbc.query("SELECT user_id FROM app_user WHERE token_hash = ?", rs ->
                rs.next() ? rs.getString(1) : null, digest(token));
        if (userId == null) {
            throw new GeneralException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }
        return userId;
    }

    public UserSummary summary(String authenticatedUser, String userId) {
        if (!authenticatedUser.equals(userId)) {
            throw new GeneralException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");
        }
        return new UserSummary(userId, stepCount(userId));
    }

    public int stepCount(String userId) {
        return jdbc.queryForObject("SELECT step_count FROM app_user WHERE user_id = ?", Integer.class, userId);
    }

    private String digest(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.US_ASCII));
            return HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public record Registration(String userId, String token) {
        @Override public String toString() { return "Registration[userId=" + userId + ", token=***]"; }
    }

    public record UserSummary(String userId, int stepCount) {}
}
