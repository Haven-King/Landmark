package dev.hephaestus.landmark.impl.names;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.hephaestus.landmark.impl.names.provider.MultiComponentProvider;
import dev.hephaestus.landmark.impl.names.provider.NameComponentProvider;
import dev.hephaestus.landmark.impl.names.provider.types.Collector;
import dev.hephaestus.landmark.impl.names.provider.types.Literal;
import dev.hephaestus.landmark.impl.names.provider.types.Reusable;
import dev.hephaestus.landmark.impl.names.provider.types.Selector;
import net.minecraft.util.Identifier;

public class NameComponentProviderSerializer {
	static NameComponentProvider from(Identifier id, JsonElement jsonElement) {
		if (!jsonElement.isJsonObject()) {
			throw new IllegalArgumentException("Name generator in illegal format: must be a JsonObject");
		}

		JsonObject jsonObject = jsonElement.getAsJsonObject();

		String type = jsonObject.get("type").getAsString();

		MultiComponentProvider provider;
		switch (type) {
			case "selector":
				provider = new Selector(id);
				break;
			case "collector":
				provider = new Collector(id);
				break;
			default:
				throw new IllegalArgumentException("Name generator in illegal format: invalid type \"" + type + "\"");
		}

		return addComponents(provider, jsonObject);
	}

	private static NameComponentProvider from(NameComponentProvider parent, JsonElement jsonElement) {
		if (jsonElement.isJsonPrimitive()) {
			JsonPrimitive primitive = jsonElement.getAsJsonPrimitive();

			if (primitive.isString()) {
				String string = jsonElement.getAsString();
				return new Literal(string, parent);
			} else if (primitive.isNumber()) {
				return new Reusable(primitive.getAsInt(), parent);
			} else {
				throw new IllegalArgumentException("Name generator in illegal format: booleans unsupported");
			}
		} else if (jsonElement.isJsonObject()) {
			JsonObject jsonObject = (JsonObject) jsonElement;

			String type = jsonObject.get("type").getAsString();

			MultiComponentProvider provider;
			switch (type) {
				case "selector":
					provider = new Selector(parent);
					break;
				case "collector":
					provider = new Collector(parent);
					break;
				default:
					throw new IllegalArgumentException("Name generator in illegal format: invalid type \"" + type + "\"");
			}

			return addComponents(provider, jsonObject);
		} else if (jsonElement.isJsonArray()) {
			Selector selector = new Selector(parent);
			jsonElement.getAsJsonArray().forEach((element) -> selector.addComponent(from(selector, element)));

			return selector;
		} else {
			throw new IllegalArgumentException("Name generator in illegal format: must be a JsonObject");
		}
	}

	private static NameComponentProvider addComponents(MultiComponentProvider multiComponentProvider, JsonObject jsonObject) {
		if (jsonObject.has("reusable_components")) {
			JsonElement reusableComponents = jsonObject.get("reusable_components");
			if (!reusableComponents.isJsonArray()) {
				throw new IllegalArgumentException("Name generator in illegal format: reusable_components must be a JsonArray");
			}

			((JsonArray)reusableComponents).forEach((element) ->
					multiComponentProvider.addReusableComponent(from(multiComponentProvider, element))
			);
		}

		if (jsonObject.has("components")) {
			JsonElement components = jsonObject.get("components");
			if (!components.isJsonArray()) {
				throw new IllegalArgumentException("Name generator in illegal format: components must be a JsonArray");
			}

			((JsonArray)components).forEach((element) ->
					multiComponentProvider.addComponent(from(multiComponentProvider, element))
			);
		}

		return multiComponentProvider;
	}
}
