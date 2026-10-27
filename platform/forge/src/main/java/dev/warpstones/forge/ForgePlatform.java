package dev.warpstones.forge;

import dev.warpstones.Platform;
import net.minecraftforge.fml.ModList;

public final class ForgePlatform implements Platform {
    @Override
    public String name() {
        return "Forge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }
}
