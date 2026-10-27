package dev.warpstones.core.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ConfigSchema {
    private final Map<String, ConfigKey<?>> byPath = new LinkedHashMap<>();

    public ConfigSchema(List<ConfigKey<?>> keys) {
        for (ConfigKey<?> key : keys) {
            if (byPath.put(key.path(), key) != null) {
                throw new IllegalArgumentException("Duplicate config key: " + key.path());
            }
        }
        for (String path : byPath.keySet()) {
            for (String other : byPath.keySet()) {
                if (other.startsWith(path + ".")) {
                    throw new IllegalArgumentException("Config key " + path + " is also the table of " + other);
                }
            }
        }
    }

    public List<ConfigKey<?>> keys() {
        return List.copyOf(byPath.values());
    }

    public List<ConfigKey<?>> keys(ConfigScope scope) {
        return byPath.values().stream().filter(k -> k.scope() == scope).toList();
    }

    public boolean contains(String path, ConfigScope scope) {
        ConfigKey<?> key = byPath.get(path);
        return key != null && key.scope() == scope;
    }
}
