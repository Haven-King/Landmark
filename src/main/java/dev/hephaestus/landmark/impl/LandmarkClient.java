package dev.hephaestus.landmark.impl;

import dev.hephaestus.landmark.impl.world.LandmarkTrackingComponent;
import me.sargunvohra.mcmods.autoconfig1u.AutoConfig;
import me.sargunvohra.mcmods.autoconfig1u.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public class LandmarkClient implements ClientModInitializer {
	public static final LandmarkTrackingComponent TRACKER = new LandmarkTrackingComponent(null);
	public static LandmarkConfig CONFIG;

	@Override
	@Environment(EnvType.CLIENT)
	public void onInitializeClient() {
		AutoConfig.register(LandmarkConfig.class, JanksonConfigSerializer::new);
		CONFIG = AutoConfig.getConfigHolder(LandmarkConfig.class).getConfig();
	}
}
