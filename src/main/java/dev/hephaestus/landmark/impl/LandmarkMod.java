package dev.hephaestus.landmark.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import dev.hephaestus.landmark.impl.landmarks.LandmarkHandler;
import dev.hephaestus.landmark.impl.names.NameGenerator;

import net.minecraft.util.Identifier;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ModInitializer;

public class LandmarkMod implements ModInitializer, ClientModInitializer {
	public static final String MODID = "landmark";
	public static final Logger LOG = LogManager.getLogger("Landmark");

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
		LandmarkHandler.initClient();
	}
}
