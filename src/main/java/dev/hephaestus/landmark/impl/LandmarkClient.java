package dev.hephaestus.landmark.impl;

import dev.hephaestus.landmark.impl.client.DeedEditScreen;
import dev.hephaestus.landmark.impl.client.LandmarkNameHandler;
import dev.hephaestus.landmark.impl.item.DeedItem;
import dev.hephaestus.landmark.impl.landmarks.LandmarkTracker;
import me.sargunvohra.mcmods.autoconfig1u.AutoConfig;
import me.sargunvohra.mcmods.autoconfig1u.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;

public class LandmarkClient implements ClientModInitializer {
    public static final LandmarkTracker TRACKER = new LandmarkTracker(null);
    public static LandmarkConfig CONFIG;

    @Override
    @Environment(EnvType.CLIENT)
    public void onInitializeClient() {
        LandmarkNameHandler.init();
        AutoConfig.register(LandmarkConfig.class, JanksonConfigSerializer::new);
        CONFIG = AutoConfig.getConfigHolder(LandmarkConfig.class).getConfig();

        ClientSidePacketRegistry.INSTANCE.register(DeedItem.DEED_OPEN_EDIT_SCREEN, DeedEditScreen::open);
        ClientSidePacketRegistry.INSTANCE.register(LandmarkTracker.LANDMARK_SYNC_ID, LandmarkTracker::read);
    }
}
