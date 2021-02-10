package dev.hephaestus.landmark.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Lifecycle;
import dev.hephaestus.landmark.impl.LandmarkMod;
import dev.hephaestus.landmark.impl.names.NameGenerator;
import dev.hephaestus.landmark.impl.names.provider.NameComponentProvider;
import dev.hephaestus.landmark.impl.names.provider.types.Translatable;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.structure.StructureStart;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.Pair;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.world.ServerWorldAccess;
import org.jetbrains.annotations.ApiStatus;

import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;

public class LandmarkTypeRegistry {
	private static final RegistryKey<Registry<LandmarkType>> KEY = RegistryKey.ofRegistry(LandmarkMod.id("landmarks"));
	private static final SimpleRegistry<LandmarkType> REGISTRY = FabricRegistryBuilder.createSimple(LandmarkType.class, LandmarkMod.id("landmarks")).buildAndRegister();

	public static void register(LandmarkType landmarkType) {
		if (REGISTRY.get(landmarkType.getId()) == null) {
			REGISTRY.add(RegistryKey.of(KEY, landmarkType.getId()), landmarkType, Lifecycle.stable());
		}
	}

	public static LandmarkType get(Identifier id) {
		return REGISTRY.get(id);
	}

	public static Iterable<LandmarkType> getRegistered() {
		return REGISTRY;
	}

	public static LandmarkType get(StructureStart<?> structureStart, ServerWorldAccess world) {
		Pair<Integer, LandmarkType> result = null;

		for (LandmarkType type : REGISTRY) {
			Optional<Pair<Integer, LandmarkType>> query = type.test(world, structureStart.getPos(), structureStart);

			if (query.isPresent() && (result == null || query.get().getLeft() < result.getLeft())) {
				result = query.get();
			}
		}

		return result == null ? null : result.getRight();
	}

	@ApiStatus.Internal
    public static void init() {
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return LandmarkMod.id("landmark", "loader");
            }

            @Override
            public void apply(ResourceManager manager) {
                Collection<Identifier> resources = manager.findResources("landmarks", (string -> string.endsWith(".json")));

                int registered = 0;

                for (Identifier resource : resources) {
                    JsonParser parser = new JsonParser();

                    try {
                        JsonElement jsonElement = parser.parse(new InputStreamReader(manager.getResource(resource).getInputStream()));

                        Identifier id = new Identifier(
                                resource.getNamespace(),
                                resource.getPath().substring(
                                        resource.getPath().indexOf("landmarks/") + 10,
                                        resource.getPath().indexOf(".json")
                                )
                        );

                        JsonObject jsonObject = jsonElement.getAsJsonObject();
                        LandmarkLocationPredicate predicate = LandmarkLocationPredicate.fromJson(jsonObject.get("location"));

                        TextColor color = TextColor.fromFormatting(Formatting.WHITE);

                        if (jsonObject.has("color")) {
                            color = TextColor.parse(jsonObject.get("color").getAsString());
                        }

                        LandmarkType type = new LandmarkType(id, predicate, color);

                        if (jsonObject.has("name_generator")) {
                            JsonElement nameGenerator = jsonObject.get("name_generator");

                            if (nameGenerator.isJsonPrimitive() && nameGenerator.getAsJsonPrimitive().isString()) {
                                type.addNameGenerator(new Identifier(jsonObject.get("name_generator").getAsString()));
                            } else if (nameGenerator.isJsonArray()) {
                                for (JsonElement element : nameGenerator.getAsJsonArray()) {
                                    if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                                        type.addNameGenerator(new Identifier(element.getAsString()));
                                    }
                                }
                            }
                        }

                        if (jsonObject.has("name")) {
                            NameComponentProvider generator = NameGenerator.register(new Translatable(id, JsonHelper.getString(jsonObject, "name")));
                            type.addNameGenerator(generator.getId());
                        }

                        register(type);
                        registered++;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                LandmarkMod.LOG.info("Registered " + registered + " landmarks");
            }
        });
    }
}
