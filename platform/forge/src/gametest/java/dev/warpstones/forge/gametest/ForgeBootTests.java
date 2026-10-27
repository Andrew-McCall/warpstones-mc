package dev.warpstones.forge.gametest;

import dev.warpstones.core.Warpstones;
import dev.warpstones.gametest.BootTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Warpstones.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ForgeBootTests {
    private ForgeBootTests() {
    }

    @GameTest(template = BootTests.EMPTY)
    public static void modIsLoaded(GameTestHelper helper) {
        BootTests.modIsLoaded(helper);
    }
}
