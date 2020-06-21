package dev.hephaestus.landmark.api;

import java.util.Collection;
import java.util.HashMap;

import com.mojang.serialization.Lifecycle;
import dev.hephaestus.landmark.impl.LandmarkMod;

import it.unimi.dsi.fastutil.Hash;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.feature.StructureFeature;

public class LandmarkTypeRegistry {
	private static final RegistryKey<Registry<LandmarkType>> KEY = RegistryKey.ofRegistry(LandmarkMod.id("landmarks"));
	private static final SimpleRegistry<LandmarkType> REGISTRY = FabricRegistryBuilder.createSimple(LandmarkType.class, LandmarkMod.id("landmarks")).buildAndRegister();
	private static final HashMap<StructureFeature<?>, LandmarkType> LOOKUPS = new HashMap<>();

	public static void register(LandmarkType landmarkType) {
		if (!REGISTRY.containsId(landmarkType.getId())) {
			REGISTRY.add(RegistryKey.of(KEY, landmarkType.getId()), landmarkType);
			if (landmarkType.getFeature() != null) {
				LOOKUPS.put(landmarkType.getFeature(), landmarkType);
			}
		}
	}

	public static LandmarkType get(Identifier id) {
		return REGISTRY.get(id);
	}

	public static Collection<Identifier> getRegistered() {
		return REGISTRY.getIds();
	}

	public static LandmarkType get(StructureStart<?> structureStart, ServerWorldAccess world) {
		for (LandmarkType type : REGISTRY) {
			if (type.test(structureStart, structureStart.getPos(), world)) {
				return type;
			}
		}

		return null;
	}
}
