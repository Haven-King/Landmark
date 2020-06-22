package dev.hephaestus.landmark.impl;

import dev.hephaestus.landmark.impl.client.DeedEditScreen;
import dev.hephaestus.landmark.impl.client.LandmarkNameHandler;
import dev.hephaestus.landmark.impl.item.DeedItem;
import dev.hephaestus.landmark.impl.world.LandmarkTrackingComponent;
import me.sargunvohra.mcmods.autoconfig1u.AutoConfig;
import me.sargunvohra.mcmods.autoconfig1u.serializer.JanksonConfigSerializer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;

public class LandmarkClient implements ClientModInitializer {
	public static final LandmarkTrackingComponent TRACKER = new LandmarkTrackingComponent(null);
	public static LandmarkConfig CONFIG;

	@Override
	@Environment(EnvType.CLIENT)
	public void onInitializeClient() {
		LandmarkNameHandler.init();
		AutoConfig.register(LandmarkConfig.class, JanksonConfigSerializer::new);
		CONFIG = AutoConfig.getConfigHolder(LandmarkConfig.class).getConfig();

		ClientSidePacketRegistry.INSTANCE.register(DeedItem.DEED_OPEN_EDIT_SCREEN, DeedEditScreen::open);
		ClientSidePacketRegistry.INSTANCE.register(LandmarkTrackingComponent.LANDMARK_SYNC_ID, LandmarkTrackingComponent::read);
	}
}
