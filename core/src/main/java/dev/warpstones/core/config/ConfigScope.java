package dev.warpstones.core.config;

public enum ConfigScope {
    COMMON("warpstones-common.toml"),
    SERVER("warpstones-server.toml"),
    CLIENT("warpstones-client.toml");

    private final String fileName;

    ConfigScope(String fileName) {
        this.fileName = fileName;
    }

    public String fileName() {
        return fileName;
    }
}
