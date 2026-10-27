package dev.warpstones.core.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.core.io.ParsingMode;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** One config file. Unknown keys are kept so that writing the file back never loses a user's settings. */
public final class ConfigFile {
    private final CommentedConfig document;
    private final ConfigValues values;

    private ConfigFile(CommentedConfig document, ConfigValues values) {
        this.document = document;
        this.values = values;
    }

    public static ConfigFile read(ConfigSchema schema, ConfigScope scope, String text, Consumer<String> warn) {
        CommentedConfig document = newDocument();
        try {
            new TomlParser().parse(text, document, ParsingMode.REPLACE);
        } catch (ParsingException e) {
            warn.accept(scope.fileName() + " is not valid TOML (" + e.getMessage() + "); using defaults");
            document = newDocument();
        }
        Map<String, Object> raw = new HashMap<>();
        for (ConfigKey<?> key : schema.keys(scope)) {
            removeBlockingValue(document, key.path(), warn, scope);
            if (document.contains(key.path())) {
                raw.put(key.path(), document.get(key.path()));
            }
        }
        warnUnknown(document, "", schema, scope, warn);
        return new ConfigFile(document, ConfigValues.load(schema, scope, raw, warn));
    }

    public static ConfigFile defaults(ConfigSchema schema, ConfigScope scope) {
        return new ConfigFile(newDocument(), ConfigValues.defaults(schema, scope));
    }

    public ConfigValues values() {
        return values;
    }

    public String write() {
        for (ConfigKey<?> key : values.schema().keys(values.scope())) {
            document.set(key.path(), values.values().get(key.path()));
            String allowed = "Allowed: " + key.allowed() + ". Default: " + key.defaultValue();
            document.setComment(key.path(), key.comment().isEmpty() ? allowed : key.comment() + "\n" + allowed);
        }
        return new TomlWriter().writeToString(document);
    }

    private static CommentedConfig newDocument() {
        return TomlFormat.instance().createConfig(LinkedHashMap::new);
    }

    // "access = 5" makes every "access.*" key unreachable, so the plain value has to go.
    private static void removeBlockingValue(CommentedConfig document, String path, Consumer<String> warn, ConfigScope scope) {
        List<String> parts = List.of(path.split("\\."));
        for (int i = 1; i < parts.size(); i++) {
            List<String> prefix = parts.subList(0, i);
            Object value = document.get(prefix);
            if (value == null) {
                return;
            }
            if (!(value instanceof Config)) {
                warn.accept(scope.fileName() + ": " + String.join(".", prefix) + " blocks " + path + " and was removed");
                document.remove(prefix);
                return;
            }
        }
    }

    private static void warnUnknown(Config config, String prefix, ConfigSchema schema, ConfigScope scope, Consumer<String> warn) {
        for (Config.Entry entry : config.entrySet()) {
            String path = prefix + entry.getKey();
            if (entry.getValue() instanceof Config table) {
                warnUnknown(table, path + ".", schema, scope, warn);
            } else if (!schema.contains(path, scope)) {
                warn.accept(scope.fileName() + ": unknown key " + path + " kept as is");
            }
        }
    }
}
