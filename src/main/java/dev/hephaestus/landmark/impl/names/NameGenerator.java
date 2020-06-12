package dev.hephaestus.landmark.impl.names;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Lifecycle;
import dev.hephaestus.landmark.impl.LandmarkMod;
import dev.hephaestus.landmark.impl.names.provider.NameComponentProvider;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.registry.SimpleRegistry;

import java.io.InputStreamReader;
import java.util.Collection;

public class NameGenerator {
	private static final RegistryKey<Registry<NameComponentProvider>> KEY = RegistryKey.ofRegistry(LandmarkMod.id("name", "providers"));
	private static final SimpleRegistry<NameComponentProvider> REGISTRY = new SimpleRegistry<>(KEY, Lifecycle.stable());

	public static void register(NameComponentProvider provider) {
		REGISTRY.add(RegistryKey.of(KEY, provider.getId()), provider);
	}

	public static String generate(Identifier id) {
		NameComponentProvider provider = REGISTRY.get(id);
		if (provider == null) {
			throw new IllegalArgumentException("Name provider not registered for \"" + id.toString() + "\"");
		}

		return provider.generateComponent();
	}

	public static void init() {
		ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
			@Override
			public Identifier getFabricId() {
				return LandmarkMod.id("landmark", "loader");
			}

			@Override
			public void apply(ResourceManager manager) {
				Collection<Identifier> resources = manager.findResources("name_generators", (string -> string.endsWith(".json")));

				int registered = 0;
				for (Identifier id : resources) {
					JsonParser parser = new JsonParser();
					try {
						JsonElement jsonElement = parser.parse(new InputStreamReader(manager.getResource(id).getInputStream()));

						register(NameComponentProviderSerializer.from(id, jsonElement));
						registered++;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				LandmarkMod.LOG.info("Registered " + registered + " name generators");
			}
		});
	}
}
