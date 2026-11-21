package net.kdt.pojavlaunch;

public final class DiscordPresence {

    static {
        System.loadLibrary("pojavexec");
    }

    private DiscordPresence() {}

    public static native void nativeInitialize();

    public static native void nativeDisconnect();

    public static void initialize() {
        nativeInitialize();
    }

    public static void disconnect() {
        nativeDisconnect();
    }
}
