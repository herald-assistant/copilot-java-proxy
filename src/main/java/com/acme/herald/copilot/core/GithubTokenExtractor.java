package com.acme.herald.copilot.core;

import jakarta.servlet.http.HttpServletRequest;

public final class GithubTokenExtractor {

    private GithubTokenExtractor() {}

    public static String extract(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.toLowerCase().startsWith("bearer ")) {
            return auth.substring("bearer ".length()).trim();
        }
        String x = request.getHeader("X-GitHub-Token");
        if (x != null) return x.trim();
        return null;
    }

    public static void validateOrThrow(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Missing GitHub token (Authorization: Bearer ... or X-GitHub-Token).");
        }
        // minimalny sanity check – bez “heurystyk PAT” (bo format się zmienia)
        if (token.contains(" ") || token.length() < 20) {
            throw new IllegalArgumentException("Invalid GitHub token format.");
        }
    }
}