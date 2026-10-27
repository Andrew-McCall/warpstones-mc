package dev.warpstones;

import java.util.ServiceLoader;

public interface Platform {
    Platform INSTANCE = ServiceLoader.load(Platform.class).findFirst()
            .orElseThrow(() -> new IllegalStateException("No Warpstones platform implementation"));

    String name();

    boolean isModLoaded(String modId);
}
