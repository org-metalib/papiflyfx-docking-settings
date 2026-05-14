package org.metalib.papifly.fx.settings.api;

public final class SecretKeyNames {

    private SecretKeyNames() {
    }

    public static String settingsKey(String provider, String name) {
        return "settings:" + provider + ":" + name;
    }

    public static String githubPat() {
        return "github:pat";
    }

    public static String oauthRefreshToken(String providerId, String subject) {
        return "login:oauth:refresh:" + providerId + ":" + subject;
    }

    public static String vaultKey(String moduleId) {
        return moduleId + ":vault:key";
    }

    public static String mcpAuthToken(String serverName) {
        return "mcp:" + serverName + ":auth-token";
    }
}
