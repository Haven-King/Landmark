package dev.hephaestus.landmark.api;

import java.util.Collection;

import com.mojang.serialization.Lifecycle;
import dev.hephaestus.landmark.impl.LandmarkMod;

import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.registry.SimpleRegistry;

public class LandmarkTypeRegistry {
	private static final RegistryKey<Registry<LandmarkType>> KEY = RegistryKey.ofRegistry(LandmarkMod.id("landmarks"));
	private static final SimpleRegistry<LandmarkType> REGISTRY = new SimpleRegistry<>(KEY, Lifecycle.stable());

	public static void register(LandmarkType landmarkType) {
		if (!REGISTRY.containsId(landmarkType.getId())) {
			REGISTRY.add(RegistryKey.of(KEY, landmarkType.getId()), landmarkType);
		}
	}

	public static LandmarkType get(Identifier id) {
		return REGISTRY.get(id);
	}

	public static Collection<Identifier> getRegistered() {
		return REGISTRY.getIds();
	}
}
