package dev.hephaestus.landmark.impl;

import dev.hephaestus.landmark.impl.landmarks.LandmarkHandler;
import dev.hephaestus.landmark.impl.names.NameGenerator;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LandmarkMod implements ModInitializer {
	public static final String MODID = "landmark";
	public static final Logger LOG = LogManager.getLogger("Landmark");

	@Override
	public void onInitialize() {
		NameGenerator.init();
		LandmarkHandler.init();
	}

	public static Identifier id(String... path) {
		return new Identifier(MODID, String.join(".", path));
	}
}
