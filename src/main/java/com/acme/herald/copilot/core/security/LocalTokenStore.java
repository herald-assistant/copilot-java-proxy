package com.acme.herald.copilot.core.security;

import java.io.IOException;
import java.nio.file.*;
import java.util.Optional;

public class LocalTokenStore {

    private final Path tokenFile;

    public LocalTokenStore() {
        this.tokenFile = Path.of(
                System.getProperty("user.home"),
                ".herald-copilot-connector",
                "copilot-token.dpapi"
        );
    }

    public boolean exists() {
        return Files.exists(tokenFile);
    }

    public void savePlainToken(String token) throws IOException {
        Files.createDirectories(tokenFile.getParent());
        String enc = DpapiCrypto.encryptToBase64(token);
        Files.writeString(tokenFile, enc,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );
    }

    public Optional<String> loadPlainToken() throws IOException {
        if (!exists()) return Optional.empty();
        String enc = Files.readString(tokenFile).trim();
        if (enc.isEmpty()) return Optional.empty();
        return Optional.of(DpapiCrypto.decryptFromBase64(enc));
    }

    public void delete() throws IOException {
        Files.deleteIfExists(tokenFile);
    }
}