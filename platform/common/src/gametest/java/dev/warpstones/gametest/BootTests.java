package dev.warpstones.gametest;

import dev.warpstones.Platform;
import dev.warpstones.core.Warpstones;
import net.minecraft.gametest.framework.GameTestHelper;

public final class BootTests {
    public static final String EMPTY = "empty";

    private BootTests() {
    }

    public static void modIsLoaded(GameTestHelper helper) {
        helper.assertTrue(Platform.INSTANCE.isModLoaded(Warpstones.MOD_ID), Warpstones.MOD_ID + " is not loaded");
        helper.succeed();
    }
}
