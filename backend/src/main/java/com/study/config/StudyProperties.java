package com.study.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "study")
public class StudyProperties {
  private String mdRoot = "md";
  private String metaPath = ".study-meta.json";
  private String adminPassword = "";

  public String getMdRoot() {
    return mdRoot;
  }

  public void setMdRoot(String mdRoot) {
    this.mdRoot = mdRoot;
  }

  public String getMetaPath() {
    return metaPath;
  }

  public void setMetaPath(String metaPath) {
    this.metaPath = metaPath;
  }

  public String getAdminPassword() {
    return adminPassword;
  }

  public void setAdminPassword(String adminPassword) {
    this.adminPassword = adminPassword;
  }
}
