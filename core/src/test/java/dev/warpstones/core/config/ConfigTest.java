package dev.warpstones.core.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class ConfigTest {
    private static final ConfigSchema SCHEMA = ConfigKeys.SCHEMA;

    @TestFactory
    Stream<DynamicTest> wrongTypeFallsBackToDefaultWithOneWarning() {
        return SCHEMA.keys().stream().map(key -> DynamicTest.dynamicTest(key.path(), () -> {
            List<String> warnings = new ArrayList<>();
            Object wrong = key.defaultValue() instanceof String ? 7L : "wrong";
            ConfigValues values = ConfigValues.load(SCHEMA, key.scope(), Map.of(key.path(), wrong), warnings::add);
            assertThat(values.get(key)).isEqualTo(key.defaultValue());
            assertThat(warnings).singleElement().asString().contains(key.path());
        }));
    }

    @Test
    void outOfRangeFallsBackToDefault() {
        List<String> warnings = new ArrayList<>();
        ConfigValues values = ConfigValues.load(SCHEMA, ConfigScope.SERVER,
                Map.of("access.nameMaxLength", 0L, "rot.damageChanceSource", 1.5), warnings::add);
        assertThat(values.get(ConfigKeys.NAME_MAX_LENGTH)).isEqualTo(32);
        assertThat(values.get(ConfigKeys.ROT_CHANCE_SOURCE)).isEqualTo(0.12);
        assertThat(warnings).hasSize(2);
    }

    @Test
    void onlyDeclaredKeysAreSynced() {
        assertThat(ConfigValues.defaults(SCHEMA, ConfigScope.SERVER).syncedValues())
                .containsOnlyKeys("access.nameMaxLength");
    }

    @Test
    void defaultsSurviveWriteAndRead() {
        for (ConfigScope scope : ConfigScope.values()) {
            List<String> warnings = new ArrayList<>();
            String text = ConfigFile.defaults(SCHEMA, scope).write();
            assertThat(ConfigFile.read(SCHEMA, scope, text, warnings::add).values().values())
                    .isEqualTo(ConfigValues.defaults(SCHEMA, scope).values());
            assertThat(warnings).isEmpty();
        }
    }

    @Test
    void writtenFileDocumentsEachKey() {
        assertThat(ConfigFile.defaults(SCHEMA, ConfigScope.SERVER).write())
                .contains("Allowed: a whole number from 1 to 256. Default: 32")
                .contains("nameMaxLength = 32");
    }

    @Test
    void unknownKeysAreKept() {
        List<String> warnings = new ArrayList<>();
        ConfigFile file = ConfigFile.read(SCHEMA, ConfigScope.SERVER, "[access]\nfuture = \"x\"\n", warnings::add);
        assertThat(warnings).singleElement().asString().contains("access.future");
        assertThat(file.write()).contains("future = \"x\"");
    }

    @Test
    void plainValueBlockingATableIsRemoved() {
        List<String> warnings = new ArrayList<>();
        ConfigFile file = ConfigFile.read(SCHEMA, ConfigScope.SERVER, "rot = 5\n", warnings::add);
        assertThat(file.values().get(ConfigKeys.ROT_CHANCE_SOURCE)).isEqualTo(0.12);
        assertThat(warnings).singleElement().asString().contains("rot");
        assertThat(ConfigFile.read(SCHEMA, ConfigScope.SERVER, file.write(), w -> { }).values().values())
                .isEqualTo(file.values().values());
    }

    @Test
    void schemaRejectsClashingKeys() {
        ConfigKey<Boolean> a = ConfigKey.bool("a.b", ConfigScope.SERVER, true);
        assertThatThrownBy(() -> new ConfigSchema(List.of(a, a))).hasMessageContaining("Duplicate");
        assertThatThrownBy(() -> new ConfigSchema(List.of(a, ConfigKey.bool("a.b.c", ConfigScope.SERVER, true))))
                .hasMessageContaining("table");
    }

    @Test
    void keysRejectBadDefinitions() {
        assertThatThrownBy(() -> ConfigKey.intRange("a.b", ConfigScope.SERVER, 1, 5, 9)).hasMessageContaining("Default");
        assertThatThrownBy(() -> ConfigKey.bool("a.b", ConfigScope.CLIENT, true).synced()).hasMessageContaining("synced");
        assertThatThrownBy(() -> ConfigKey.bool("nodot", ConfigScope.SERVER, true)).hasMessageContaining("path");
    }

    @Property
    void readingIsStableUnderRewrite(@ForAll("rawServerValues") Map<String, Object> raw) {
        CommentedConfig doc = TomlFormat.newConfig();
        raw.forEach(doc::set);
        String first = ConfigFile.read(SCHEMA, ConfigScope.SERVER, TomlFormat.instance().createWriter().writeToString(doc), w -> { }).write();
        List<String> warnings = new ArrayList<>();
        ConfigFile second = ConfigFile.read(SCHEMA, ConfigScope.SERVER, first, warnings::add);
        assertThat(second.write()).isEqualTo(first);
        assertThat(warnings).isEmpty();
    }

    @Property
    void malformedTextNeverThrows(@ForAll String text) {
        ConfigFile file = ConfigFile.read(SCHEMA, ConfigScope.SERVER, text, w -> { });
        assertThat(file.values().values()).containsOnlyKeys(ConfigValues.defaults(SCHEMA, ConfigScope.SERVER).values().keySet());
    }

    @Provide
    Arbitrary<Map<String, Object>> rawServerValues() {
        Arbitrary<Object> value = Arbitraries.oneOf(
                Arbitraries.of(true, false).map(Object.class::cast),
                Arbitraries.longs().between(-1000, 1000).map(Object.class::cast),
                Arbitraries.doubles().between(-2, 2).map(Object.class::cast),
                Arbitraries.of("personal", "standard", "team", "public", "").map(Object.class::cast));
        Arbitrary<String> path = Arbitraries.of(SCHEMA.keys(ConfigScope.SERVER).stream().map(ConfigKey::path).toList());
        return Arbitraries.maps(path, value).ofMaxSize(SCHEMA.keys(ConfigScope.SERVER).size());
    }
}
