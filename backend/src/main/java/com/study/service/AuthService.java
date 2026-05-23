package com.study.service;

import com.study.config.StudyProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
  private static final long SESSION_MILLIS = 1000L * 60 * 60 * 12;

  private final StudyProperties properties;
  private final SecureRandom secureRandom = new SecureRandom();
  private final Map<String, Long> sessions = new ConcurrentHashMap<>();

  public AuthService(StudyProperties properties) {
    this.properties = properties;
  }

  public String login(String password) {
    String configured = properties.getAdminPassword();
    if (configured == null || configured.isBlank()) {
      throw new IllegalStateException("管理密码未配置，请在 .env 中设置 STUDY_ADMIN_PASSWORD。");
    }
    if (!timingSafeEquals(password, configured)) {
      throw new IllegalArgumentException("密码不正确。");
    }
    byte[] bytes = new byte[32];
    secureRandom.nextBytes(bytes);
    String token = HexFormat.of().formatHex(bytes);
    sessions.put(token, Instant.now().toEpochMilli() + SESSION_MILLIS);
    return token;
  }

  public boolean verify(String authorization) {
    String token = authorization != null && authorization.startsWith("Bearer ")
        ? authorization.substring("Bearer ".length())
        : "";
    Long expiresAt = sessions.get(token);
    if (expiresAt == null || expiresAt < Instant.now().toEpochMilli()) {
      sessions.remove(token);
      return false;
    }
    sessions.put(token, Instant.now().toEpochMilli() + SESSION_MILLIS);
    return true;
  }

  private boolean timingSafeEquals(String left, String right) {
    byte[] leftBytes = String.valueOf(left).getBytes(StandardCharsets.UTF_8);
    byte[] rightBytes = String.valueOf(right).getBytes(StandardCharsets.UTF_8);
    return leftBytes.length == rightBytes.length && MessageDigest.isEqual(leftBytes, rightBytes);
  }
}
