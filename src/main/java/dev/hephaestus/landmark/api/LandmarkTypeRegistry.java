package dev.hephaestus.landmark.api;

import java.util.Iterator;

import dev.hephaestus.landmark.impl.LandmarkMod;

import net.minecraft.structure.StructureStart;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.world.ServerWorldAccess;

import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;

public class LandmarkTypeRegistry {
	private static final RegistryKey<Registry<LandmarkType>> KEY = RegistryKey.ofRegistry(LandmarkMod.id("landmarks"));
	private static final SimpleRegistry<LandmarkType> REGISTRY = FabricRegistryBuilder.createSimple(LandmarkType.class, LandmarkMod.id("landmarks")).buildAndRegister();

	public static void register(LandmarkType landmarkType) {
		if (!REGISTRY.containsId(landmarkType.getId())) {
			REGISTRY.add(RegistryKey.of(KEY, landmarkType.getId()), landmarkType);
		}
	}

	public static LandmarkType get(Identifier id) {
		return REGISTRY.get(id);
	}

	public static Iterator<LandmarkType> getRegistered() {
		return REGISTRY.iterator();
	}

	public static LandmarkType get(StructureStart<?> structureStart, ServerWorldAccess world) {
		Pair<Integer, LandmarkType> result = null;

		for (LandmarkType type : REGISTRY) {
			Pair<Integer, LandmarkType> query = type.test(structureStart, structureStart.getPos(), world);

			if (query.getLeft() >= 0 && (result == null || query.getLeft() < result.getLeft())) {
				result = query;
			}
		}

		return result == null ? null : result.getRight();
	}
}
