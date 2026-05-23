package com.study;

import com.study.config.StudyProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(StudyProperties.class)
public class StudyApplication {

  public static void main(String[] args) {
    loadDotenv();
    SpringApplication.run(StudyApplication.class, args);
  }

  private static void loadDotenv() {
    List<Path> candidates = List.of(Path.of(".env"), Path.of("..", ".env"));
    for (Path candidate : candidates) {
      if (!Files.isRegularFile(candidate)) {
        continue;
      }
      try {
        for (String rawLine : Files.readAllLines(candidate, StandardCharsets.UTF_8)) {
          String line = rawLine.trim();
          if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
            continue;
          }
          int splitAt = line.indexOf('=');
          String key = line.substring(0, splitAt).trim();
          String value = line.substring(splitAt + 1).trim();
          if (!key.isEmpty() && System.getenv(key) == null && System.getProperty(key) == null) {
            System.setProperty(key, value);
          }
        }
        return;
      } catch (IOException ignored) {
        return;
      }
    }
  }
}
