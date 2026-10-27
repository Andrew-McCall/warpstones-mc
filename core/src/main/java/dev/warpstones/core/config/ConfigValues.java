package dev.warpstones.core.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class ConfigValues {
    private final ConfigSchema schema;
    private final ConfigScope scope;
    private final Map<String, Object> values;

    private ConfigValues(ConfigSchema schema, ConfigScope scope, Map<String, Object> values) {
        this.schema = schema;
        this.scope = scope;
        this.values = Collections.unmodifiableMap(values);
    }

    public static ConfigValues defaults(ConfigSchema schema, ConfigScope scope) {
        return load(schema, scope, Map.of(), message -> {
        });
    }

    /** Invalid values fall back to their default with one warning each; missing values fall back silently. */
    public static ConfigValues load(ConfigSchema schema, ConfigScope scope, Map<String, ?> raw, Consumer<String> warn) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (ConfigKey<?> key : schema.keys(scope)) {
            Object value = key.defaultValue();
            if (raw.containsKey(key.path())) {
                try {
                    value = key.parse(raw.get(key.path()));
                } catch (InvalidValueException e) {
                    warn.accept(scope.fileName() + ": " + key.path() + ": " + e.getMessage()
                            + "; using the default " + key.defaultValue());
                }
            }
            values.put(key.path(), value);
        }
        return new ConfigValues(schema, scope, values);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(ConfigKey<T> key) {
        if (key.scope() != scope) {
            throw new IllegalArgumentException(key.path() + " is not a " + scope + " key");
        }
        return (T) values.get(key.path());
    }

    public Map<String, Object> values() {
        return values;
    }

    public Map<String, Object> syncedValues() {
        Map<String, Object> synced = new LinkedHashMap<>();
        for (ConfigKey<?> key : schema.keys(scope)) {
            if (key.isSynced()) {
                synced.put(key.path(), values.get(key.path()));
            }
        }
        return synced;
    }

    public ConfigSchema schema() {
        return schema;
    }

    public ConfigScope scope() {
        return scope;
    }
}
