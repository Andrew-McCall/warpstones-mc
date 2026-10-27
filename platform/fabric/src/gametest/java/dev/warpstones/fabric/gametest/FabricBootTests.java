package dev.warpstones.fabric.gametest;

import dev.warpstones.core.Warpstones;
import dev.warpstones.gametest.BootTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public final class FabricBootTests {
    private static final String EMPTY = Warpstones.MOD_ID + ":" + BootTests.EMPTY;

    @GameTest(template = EMPTY)
    public void modIsLoaded(GameTestHelper helper) {
        BootTests.modIsLoaded(helper);
    }
}
