package dev.hephaestus.landmark.api;

import com.mojang.serialization.Lifecycle;
import dev.hephaestus.landmark.impl.LandmarkMod;
import net.minecraft.predicate.entity.LocationPredicate;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.registry.SimpleRegistry;

import java.util.Collection;

public class LandmarkRegistry {
	private static final RegistryKey<Registry<Landmark>> KEY = RegistryKey.ofRegistry(LandmarkMod.id("landmarks"));
	private static final SimpleRegistry<Landmark> REGISTRY = new SimpleRegistry<>(KEY, Lifecycle.stable());

	public static void register(Identifier id, LocationPredicate predicate) {
		REGISTRY.add(RegistryKey.of(KEY, id), new Landmark(id, predicate));
	}

	public static void register(Landmark landmark) {
		REGISTRY.add(RegistryKey.of(KEY, landmark.getId()), landmark);
	}

	public static Landmark get(Identifier id) {
		return REGISTRY.get(id);
	}

	public static Collection<Identifier> getRegistered() {
		return REGISTRY.getIds();
	}
}
