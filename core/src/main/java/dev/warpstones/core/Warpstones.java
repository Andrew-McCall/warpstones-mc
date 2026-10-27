package dev.warpstones.core;

public final class Warpstones {
    public static final String MOD_ID = "warpstones";
    public static final String MOD_NAME = "Warpstones";
    public static final int PROTOCOL_VERSION = 1;

    private Warpstones() {
    }

    public static String id(String path) {
        return MOD_ID + ":" + path;
    }
}
