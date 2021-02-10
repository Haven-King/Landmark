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
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.registry.SimpleRegistry;
import org.apache.commons.lang3.text.WordUtils;

import java.io.InputStreamReader;
import java.util.Collection;

public class NameGenerator {
	private static final RegistryKey<Registry<NameComponentProvider>> KEY = RegistryKey.ofRegistry(LandmarkMod.id("name", "providers"));
	private static SimpleRegistry<NameComponentProvider> REGISTRY = new SimpleRegistry<>(KEY, Lifecycle.stable());

	public static NameComponentProvider register(NameComponentProvider provider) {
		if (REGISTRY.get(provider.getId()) == null) {
			REGISTRY.add(RegistryKey.of(KEY, provider.getId()), provider, Lifecycle.stable());
		}

		return REGISTRY.get(provider.getId());
	}

	public static MutableText generate(Identifier id) {
		NameComponentProvider provider = REGISTRY.get(id);

		if (provider == null) {
			throw new IllegalArgumentException("Name provider not registered for \"" + id.toString() + "\"");
		}

		Text text = provider.generateComponent();

		if (text instanceof TranslatableText) {
			return ((TranslatableText) text).styled(style -> style.withColor(provider.getColor()));
		} else {
			return new LiteralText(WordUtils.capitalize(provider.generateComponent().getString().trim())).styled(style -> style.withColor(provider.getColor()));
		}
	}

	public static void init() {
		ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
			@Override
			public Identifier getFabricId() {
				return LandmarkMod.id("name_generator", "loader");
			}

			@Override
			public void apply(ResourceManager manager) {
				REGISTRY = new SimpleRegistry<>(KEY, Lifecycle.stable());
				Collection<Identifier> resources = manager.findResources("name_generators", (string -> string.endsWith(".json")));

				int registered = 0;

				for (Identifier resource : resources) {
					JsonParser parser = new JsonParser();

					try {
						JsonElement jsonElement = parser.parse(new InputStreamReader(manager.getResource(resource).getInputStream()));

						Identifier id = new Identifier(
								resource.getNamespace(),
								resource.getPath().substring(
								resource.getPath().indexOf("name_generators/") + 16,
								resource.getPath().indexOf(".json")
						));

						register(NameComponentProviderSerializer.deserialize(id, jsonElement));

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
