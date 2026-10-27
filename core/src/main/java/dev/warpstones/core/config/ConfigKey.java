package dev.warpstones.core.config;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

public final class ConfigKey<T> {
    private static final Pattern PATH = Pattern.compile("[a-z][A-Za-z0-9]*(\\.[a-z][A-Za-z0-9]*)+");

    private final String path;
    private final ConfigScope scope;
    private final T defaultValue;
    private final String allowed;
    private final Function<Object, T> convert;
    private final boolean synced;
    private final String comment;

    private ConfigKey(String path, ConfigScope scope, T defaultValue, String allowed, Function<Object, T> convert,
                      boolean synced, String comment) {
        if (!PATH.matcher(path).matches()) {
            throw new IllegalArgumentException("Bad config path: " + path);
        }
        if (synced && scope != ConfigScope.SERVER) {
            throw new IllegalArgumentException("Only server keys can be synced: " + path);
        }
        if (!Objects.equals(convert.apply(defaultValue), defaultValue)) {
            throw new IllegalArgumentException("Default of " + path + " is not " + allowed);
        }
        this.path = path;
        this.scope = scope;
        this.defaultValue = defaultValue;
        this.allowed = allowed;
        this.convert = convert;
        this.synced = synced;
        this.comment = comment;
    }

    public static ConfigKey<Boolean> bool(String path, ConfigScope scope, boolean def) {
        return new ConfigKey<>(path, scope, def, "true or false",
                raw -> raw instanceof Boolean b ? b : null, false, "");
    }

    public static ConfigKey<Integer> intRange(String path, ConfigScope scope, int min, int max, int def) {
        return new ConfigKey<>(path, scope, def, "a whole number from " + min + " to " + max, raw -> {
            if (!(raw instanceof Integer || raw instanceof Long)) {
                return null;
            }
            long value = ((Number) raw).longValue();
            return value >= min && value <= max ? (int) value : null;
        }, false, "");
    }

    public static ConfigKey<Double> doubleRange(String path, ConfigScope scope, double min, double max, double def) {
        return new ConfigKey<>(path, scope, def, "a number from " + min + " to " + max, raw -> {
            if (!(raw instanceof Integer || raw instanceof Long || raw instanceof Double)) {
                return null;
            }
            double value = ((Number) raw).doubleValue();
            return value >= min && value <= max ? value : null;
        }, false, "");
    }

    public static ConfigKey<String> oneOf(String path, ConfigScope scope, String def, String... values) {
        List<String> options = List.of(values);
        return new ConfigKey<>(path, scope, def, "one of " + String.join(", ", options),
                raw -> raw instanceof String s && options.contains(s) ? s : null, false, "");
    }

    public ConfigKey<T> synced() {
        return new ConfigKey<>(path, scope, defaultValue, allowed, convert, true, comment);
    }

    public ConfigKey<T> comment(String text) {
        return new ConfigKey<>(path, scope, defaultValue, allowed, convert, synced, text);
    }

    public T parse(Object raw) throws InvalidValueException {
        T value = convert.apply(raw);
        if (value == null) {
            throw new InvalidValueException("expected " + allowed + ", got " + raw);
        }
        return value;
    }

    public String path() {
        return path;
    }

    public ConfigScope scope() {
        return scope;
    }

    public T defaultValue() {
        return defaultValue;
    }

    public String allowed() {
        return allowed;
    }

    public boolean isSynced() {
        return synced;
    }

    public String comment() {
        return comment;
    }

    @Override
    public String toString() {
        return path;
    }
}
