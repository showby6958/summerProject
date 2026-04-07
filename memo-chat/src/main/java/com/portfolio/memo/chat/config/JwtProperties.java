package com.portfolio.memo.chat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secretKey;
    private long accessTokenValidityMs;
    private long refreshTokenValidityMs;

    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

    public long getAccessTokenValidityMs() { return accessTokenValidityMs; }
    public void setAccessTokenValidityMs(long accessTokenValidityMs) { this.accessTokenValidityMs = accessTokenValidityMs; }

    public long getRefreshTokenValidityMs() { return refreshTokenValidityMs; }
    public void setRefreshTokenValidityMs(long refreshTokenValidityMs) { this.refreshTokenValidityMs = refreshTokenValidityMs; }
}
