package dev.hephaestus.landmark.api;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.predicate.NumberRange;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.feature.StructureFeature;

import java.util.*;

public class LandmarkLocationPredicate {
	private final Map<Predicate, Integer> predicates;

	public LandmarkLocationPredicate(ImmutableMap<Predicate, Integer> predicates) {
		this.predicates = predicates;
	}

	public static LandmarkLocationPredicate fromJson(JsonElement jsonElement) {
		Builder builder = new Builder();

		if (jsonElement != null && jsonElement.isJsonObject()) {
			for (Map.Entry<String, JsonElement> entry : JsonHelper.asObject(jsonElement, "location").entrySet()) {
				LandmarkLocationPredicateRegistry.apply(builder, entry.getKey(), entry.getValue());
			}
		}

		return builder.build();
	}

	public Optional<Integer> test(WorldAccess world, BlockPos pos, StructureStart<?> structureStart) {
		int result = 100;

		for (Map.Entry<Predicate, Integer> entry : this.predicates.entrySet()) {
			if (entry.getKey().test(world, pos, structureStart)) {
				result -= entry.getValue();
			} else {
				return Optional.empty();
			}
		}

		return Optional.of(result);
	}

	@FunctionalInterface
	public interface Predicate {
		static Predicate always() {
			return ((world, pos, structureStart) -> true);
		}

		boolean test(WorldAccess world, BlockPos pos, StructureStart<?> structureStart);
	}

	public static class Builder {
		private final Map<Predicate, Integer> predicates = new HashMap<>();

		public Builder add(Predicate predicate, int weight) {
			this.predicates.put(predicate, weight);
			return this;
		}

		public LandmarkLocationPredicate build() {
			return new LandmarkLocationPredicate(ImmutableMap.copyOf(predicates));
		}
	}
}
