package dev.hephaestus.landmark.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.predicate.NumberRange;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.feature.StructureFeature;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;

public class LandmarkLocationPredicateRegistry {
    private static final Map<String, Factory> FACTORIES = new HashMap<>();

    public static void register(String key, Factory factory) {
        FACTORIES.putIfAbsent(key, factory);
    }

    public static void set(String key, Factory factory) {
        FACTORIES.put(key, factory);
    }

    @ApiStatus.Internal
    public static void apply(LandmarkLocationPredicate.Builder builder, String key, JsonElement element) {
        if (FACTORIES.containsKey(key)) {
            FACTORIES.get(key).add(builder, element);
        }
    }

    public interface Factory {
        void add(LandmarkLocationPredicate.Builder builder, JsonElement element);
    }

    static {
        register("biome", ((builder, element) -> {
            Collection<Identifier> biomes = new ArrayList<>();

            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                DataResult<Identifier> biomeResult = Identifier.CODEC.parse(JsonOps.INSTANCE, element);
                biomeResult.result().ifPresent(biomes::add);
            } else if (element.isJsonArray()) {
                JsonArray biomeArray = element.getAsJsonArray();

                for (JsonElement biome : biomeArray) {
                    DataResult<Identifier> biomeResult = Identifier.CODEC.parse(JsonOps.INSTANCE, biome);
                    biomeResult.result().ifPresent(biomes::add);
                }
            } else {
                return;
            }

            builder.add((world, pos, start) -> {
                Biome biome = world.getBiome(pos);
                boolean testPassed = false;

                for (Identifier id : biomes) {
                    if (id.equals(world.getRegistryManager().get(Registry.BIOME_KEY).getId(biome))) {
                        testPassed = true;
                        break;
                    }
                }

                return testPassed;
            }, 5);
        }));

        register("dimension", ((builder, element) -> {
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                String dim = element.getAsString();
                RegistryKey<DimensionType> dimension = RegistryKey.of(Registry.DIMENSION_TYPE_KEY, new Identifier(dim));

                builder.add((world, pos, start) -> {
                    DimensionType thisDimension = world.getRegistryManager().get(Registry.DIMENSION_TYPE_KEY).get(dimension);
                    return world.getDimension() == thisDimension;
                }, 5);
            }
        }));

        register("feature", ((builder, element) -> {
            Collection<StructureFeature<?>> features = new HashSet<>();

            if (element.isJsonNull()) {
                builder.add(((world, pos, structureStart) -> structureStart == null), 0);
                return;
            } if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                features.add(StructureFeature.STRUCTURES.get(element.getAsString()));
            } else if (element.isJsonArray()) {
                for (JsonElement arrayElement : element.getAsJsonArray()) {
                    if (arrayElement.isJsonPrimitive() && arrayElement.getAsJsonPrimitive().isString()) {
                        features.add(StructureFeature.STRUCTURES.get(arrayElement.getAsString()));
                    }
                }
            }

            builder.add((world, pos, start) ->
                    start != null && start.hasChildren() && features.contains(start.getFeature()), 10);
        }));

        register("y", ((builder, element) -> {
            NumberRange.FloatRange range = NumberRange.FloatRange.fromJson(element);
            builder.add((world, p1, start) -> range.test(p1.getY()), 5);
        }));
    }
}
