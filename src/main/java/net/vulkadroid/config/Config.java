package net.vulkadroid.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.vulkadroid.Initializer;

import java.io.*;
import java.nio.file.*;

public class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Paths.get("config", "vulkadroid.json");

    private static boolean vsync = false;
    private static int windowWidth  = 1080;
    private static int windowHeight = 2400;
    private static int renderDistance = 8;
    private static boolean multiThreadedChunkBuilding = true;
    private static int chunkBuildThreads = 3;
    private static boolean indirectDrawCalls = true;
    private static boolean frustumCulling = true;

    public static void load() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            if (Files.exists(CONFIG_PATH)) {
                ConfigData data = GSON.fromJson(Files.readString(CONFIG_PATH), ConfigData.class);
                if (data != null) {
                    vsync                     = data.vsync;
                    windowWidth               = data.windowWidth;
                    windowHeight              = data.windowHeight;
                    renderDistance            = data.renderDistance;
                    multiThreadedChunkBuilding = data.multiThreadedChunkBuilding;
                    chunkBuildThreads         = data.chunkBuildThreads;
                    indirectDrawCalls         = data.indirectDrawCalls;
                    frustumCulling            = data.frustumCulling;
                }
            } else {
                save();
            }
        } catch (Exception e) {
            Initializer.LOGGER.error("Failed to load config, using defaults", e);
        }
    }

    public static void save() {
        try {
            ConfigData data = new ConfigData();
            data.vsync                      = vsync;
            data.windowWidth                = windowWidth;
            data.windowHeight               = windowHeight;
            data.renderDistance             = renderDistance;
            data.multiThreadedChunkBuilding = multiThreadedChunkBuilding;
            data.chunkBuildThreads          = chunkBuildThreads;
            data.indirectDrawCalls          = indirectDrawCalls;
            data.frustumCulling             = frustumCulling;
            Files.writeString(CONFIG_PATH, GSON.toJson(data));
        } catch (Exception e) {
            Initializer.LOGGER.error("Failed to save config", e);
        }
    }

    private static class ConfigData {
        boolean vsync = false;
        int windowWidth  = 1080;
        int windowHeight = 2400;
        int renderDistance = 8;
        boolean multiThreadedChunkBuilding = true;
        int chunkBuildThreads = 3;
        boolean indirectDrawCalls = true;
        boolean frustumCulling = true;
    }

    public static boolean isVsyncEnabled() { return vsync; }
    public static int getWindowWidth()     { return windowWidth; }
    public static int getWindowHeight()    { return windowHeight; }
    public static int getRenderDistance()  { return renderDistance; }
    public static boolean isMultiThreadedChunkBuilding() { return multiThreadedChunkBuilding; }
    public static int getChunkBuildThreads() { return chunkBuildThreads; }
    public static boolean isIndirectDrawCalls() { return indirectDrawCalls; }
    public static boolean isFrustumCulling() { return frustumCulling; }
    public static void setWindowSize(int w, int h) { windowWidth = w; windowHeight = h; }
}
