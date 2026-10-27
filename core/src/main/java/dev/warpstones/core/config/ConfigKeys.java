package dev.warpstones.core.config;

import static dev.warpstones.core.config.ConfigScope.COMMON;
import static dev.warpstones.core.config.ConfigScope.SERVER;

import java.util.List;

public final class ConfigKeys {
    public static final ConfigKey<Boolean> WAYSTONES_ENABLED =
            ConfigKey.bool("content.waystones.enabled", COMMON, true)
                    .comment("When off, waystones stay registered but are hidden and don't generate.");

    public static final ConfigKey<Boolean> FREE_IN_CREATIVE =
            ConfigKey.bool("costs.default.freeInCreative", SERVER, true);

    public static final ConfigKey<String> DEFAULT_VISIBILITY_PLACED =
            ConfigKey.oneOf("access.defaultVisibility.placed", SERVER, "standard", "personal", "standard", "team", "public")
                    .comment("\"standard\" is shown as Default.");

    public static final ConfigKey<Integer> NAME_MAX_LENGTH =
            ConfigKey.intRange("access.nameMaxLength", SERVER, 1, 256, 32).synced();

    public static final ConfigKey<Double> ROT_CHANCE_SOURCE =
            ConfigKey.doubleRange("rot.damageChanceSource", SERVER, 0.0, 1.0, 0.12)
                    .comment("Chance per teleport that the departure waystone wears down one stage.");

    public static final ConfigKey<Double> ROT_CHANCE_TARGET =
            ConfigKey.doubleRange("rot.damageChanceTarget", SERVER, 0.0, 1.0, 0.04)
                    .comment("Chance per teleport that the arrival waystone wears down one stage.");

    public static final ConfigKey<Boolean> REDSTONE_PULSE =
            ConfigKey.bool("redstone.pulseOnTeleport", SERVER, true);

    public static final ConfigKey<Integer> REDSTONE_PULSE_TICKS =
            ConfigKey.intRange("redstone.pulseTicks", SERVER, 1, 200, 4);

    public static final ConfigSchema SCHEMA = new ConfigSchema(List.of(
            WAYSTONES_ENABLED,
            FREE_IN_CREATIVE,
            DEFAULT_VISIBILITY_PLACED,
            NAME_MAX_LENGTH,
            ROT_CHANCE_SOURCE,
            ROT_CHANCE_TARGET,
            REDSTONE_PULSE,
            REDSTONE_PULSE_TICKS));

    private ConfigKeys() {
    }
}
