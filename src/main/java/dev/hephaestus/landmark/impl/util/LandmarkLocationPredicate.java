package dev.hephaestus.landmark.impl.util;

import java.util.ArrayList;
import java.util.Objects;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.hephaestus.landmark.impl.LandmarkMod;

import net.minecraft.predicate.NumberRange;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.feature.StructureFeature;

public class LandmarkLocationPredicate {
	private final NumberRange.FloatRange y;

	private final StructureFeature<?> feature;
	private final RegistryKey<DimensionType> dimension;
	private final ArrayList<Identifier> biomes = new ArrayList<>();

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

			if (jsonObject.has("y")) {
				y = NumberRange.FloatRange.fromJson(jsonObject.get("y"));
			}

			StructureFeature<?> feature = null;

			if (jsonObject.has("feature")) {
				feature = StructureFeature.STRUCTURES.get(JsonHelper.getString(jsonObject, "feature"));
			}

			RegistryKey<DimensionType> dimension = null;

			if (jsonObject.has("dimension")) {
				dimension = RegistryKey.of(Registry.DIMENSION_TYPE_KEY, new Identifier(JsonHelper.getString(jsonObject, "dimension")));
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
		this.biomes.add(id);
	}

	public int test(StructureStart<?> structureStart, BlockPos pos, ServerWorldAccess world) {
		int result = 100;

		if (this.y != null && !this.y.test((float) pos.getY())) {
			return -1;
		} else {
			result -= 5;
		}

		if (this.dimension != null) {
			DimensionType testDimension = world.getDimension();

			DimensionType thisDimension = world.getRegistryManager().get(Registry.DIMENSION_TYPE_KEY).get(this.dimension);

			if (testDimension != thisDimension) {
				return -1;
			} else {
				result -= 5;
			}
		}

		if (this.biomes.size() > 0) {
			boolean testPassed = false;
			Biome targetBiome = world.getBiome(pos);

			for (Identifier id : this.biomes) {
				if (id.equals(world.getRegistryManager().get(Registry.BIOME_KEY).getId(targetBiome))) {
					testPassed = true;
					break;
				}
			}

			if (!testPassed) {
				return -1;
			} else {
				result -= 5;
			}
		}

		if (this.feature != null) {
			if (structureStart != null && structureStart.hasChildren() && structureStart.getFeature() == this.feature) {
				result -= 10;
			} else {
				return -1;
			}
		}

		return result;
	}
}
