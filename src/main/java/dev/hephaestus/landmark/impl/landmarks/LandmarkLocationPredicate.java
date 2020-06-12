package dev.hephaestus.landmark.impl.landmarks;

import java.util.ArrayList;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.hephaestus.landmark.impl.LandmarkMod;

import net.minecraft.predicate.NumberRange;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.feature.StructureFeature;

public class LandmarkLocationPredicate {
	private final NumberRange.FloatRange y;

	private final StructureFeature<?> feature;
	private final ArrayList<Biome> biomes = new ArrayList<>();
	private final RegistryKey<DimensionType> dimension;

	public LandmarkLocationPredicate(NumberRange.FloatRange y, StructureFeature<?> feature, RegistryKey<DimensionType> dimension) {
		this.y = y;
		this.feature = feature;
		this.dimension = dimension;
	}

	public StructureFeature<?> getFeature() {
		return this.feature;
	}

	public static LandmarkLocationPredicate fromJson(JsonElement jsonElement) {
		if (jsonElement != null && jsonElement.isJsonObject()) {
			JsonObject jsonObject = JsonHelper.asObject(jsonElement, "location");

			NumberRange.FloatRange y = NumberRange.FloatRange.ANY;
			StructureFeature<?> feature = null;
			RegistryKey<DimensionType> dimension = null;

			if (jsonObject.has("y")) {
				y = NumberRange.FloatRange.fromJson(jsonObject.get("y"));
			}

			if (jsonObject.has("dimension")) {
				DataResult<Identifier> dimensionId = Identifier.CODEC.parse(JsonOps.INSTANCE, jsonObject.get("dimension"));
				dimension = dimensionId.resultOrPartial(LandmarkMod.LOG::error).map((identifer) -> RegistryKey.of(Registry.DIMENSION_TYPE_KEY, identifer)).orElse(null);
			}

			if (jsonObject.has("feature")) {
				feature = StructureFeature.STRUCTURES.get(JsonHelper.getString(jsonObject, "feature"));
			}

			LandmarkLocationPredicate predicate = new LandmarkLocationPredicate(y, feature, dimension);

			if (jsonObject.has("biome")) {
				JsonElement biomeElement = jsonObject.get("biome");

				if (biomeElement.isJsonPrimitive() && biomeElement.getAsJsonPrimitive().isString()) {
					DataResult<Identifier> biomeResult = Identifier.CODEC.parse(JsonOps.INSTANCE, biomeElement);
					predicate.addBiome(biomeResult);
				} else if (biomeElement.isJsonArray()) {
					JsonArray biomes = biomeElement.getAsJsonArray();

					for (JsonElement biome : biomes) {
						DataResult<Identifier> biomeResult = Identifier.CODEC.parse(JsonOps.INSTANCE, biome);

						predicate.addBiome(biomeResult);
					}
				}
			}

			return predicate;
		}

		return new LandmarkLocationPredicate(null, null, null);
	}

	private void addBiome(DataResult<Identifier> identifierDataResult) {
		Identifier id = identifierDataResult.resultOrPartial(LandmarkMod.LOG::error).orElse(null);

		if (Registry.BIOME.containsId(id)) {
			this.addBiome(Registry.BIOME.get(id));
		}
	}

	private void addBiome(Biome biome) {
		if (biome != null) {
			this.biomes.add(biome);
		}
	}

	public boolean test(ServerPlayerEntity playerEntity) {
		ServerWorld world = playerEntity.getServerWorld();

		if (this.y != null && !this.y.test((float) playerEntity.getY())) {
			return false;
		}

		if (this.dimension != null && this.dimension != world.getDimensionRegistryKey()) {
			return false;
		}

		BlockPos pos = playerEntity.getBlockPos();
		boolean bl = world.canSetBlock(pos);

		if (this.biomes.size() > 0) {
			boolean testPassed = false;
			Biome targetBiome = world.getBiome(pos);

			for (Biome biome : this.biomes) {
				if (biome == targetBiome) {
					testPassed = true;
					break;
				}
			}

			if (!testPassed) {
				return false;
			}
		}

		return this.feature == null || (bl && world.getStructureAccessor().method_28388(pos, true, this.feature).hasChildren());
	}
}
