package dev.hephaestus.landmark.impl;

import dev.hephaestus.landmark.impl.client.LandmarkNameHandler;
import dev.hephaestus.landmark.impl.landmarks.LandmarkHandler;
import dev.hephaestus.landmark.impl.names.NameGenerator;
import me.sargunvohra.mcmods.autoconfig1u.AutoConfig;
import me.sargunvohra.mcmods.autoconfig1u.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LandmarkMod implements ModInitializer, ClientModInitializer {
	public static final String MODID = "landmark";
	public static final String MOD_NAME = "Landmark";
	public static final Logger LOG = LogManager.getLogger(MOD_NAME);
	public static final Identifier LANDMARK_DISCOVERED_PACKET = LandmarkMod.id("packet", "discovered");

	public static LandmarkConfig CONFIG;

	public static Identifier id(String... path) {
		return new Identifier(MODID, String.join(".", path));
	}

	@Override
	public void onInitialize() {
		NameGenerator.init();
		LandmarkHandler.init();
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void onInitializeClient() {
		LandmarkNameHandler.init();
		AutoConfig.register(LandmarkConfig.class, JanksonConfigSerializer::new);
		CONFIG = AutoConfig.getConfigHolder(LandmarkConfig.class).getConfig();
	}
}
