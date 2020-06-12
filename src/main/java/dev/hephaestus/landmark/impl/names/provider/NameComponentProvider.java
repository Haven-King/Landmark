package dev.hephaestus.landmark.impl.names.provider;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.hephaestus.landmark.impl.names.provider.types.Collector;
import dev.hephaestus.landmark.impl.names.provider.types.Literal;
import dev.hephaestus.landmark.impl.names.provider.types.Reusable;
import dev.hephaestus.landmark.impl.names.provider.types.Selector;
import net.minecraft.util.Identifier;

import java.util.ArrayList;

public abstract class NameComponentProvider {
	private final Identifier id;
	private final ArrayList<NameComponentProvider> reusableComponents;

	public NameComponentProvider(Identifier id) {
		this.id = id;
		this.reusableComponents = new ArrayList<>();
	}

	public NameComponentProvider(NameComponentProvider parent) {
		this.id = parent.id;
		this.reusableComponents = parent.reusableComponents;
	}

	public void addReusableComponent(NameComponentProvider provider) {
		this.reusableComponents.add(provider);
	}

	public Identifier getId() {
		return this.id;
	}

	public String generateComponent(int reusableComponentIndex) {
		if ((reusableComponentIndex > this.reusableComponents.size() || reusableComponentIndex < 0)) {
			throw new IllegalArgumentException("Invalid reusableComponent number \"" + reusableComponentIndex + "\" in " + this.id.toString());
		}

		return this.reusableComponents.get(reusableComponentIndex).generateComponent();
	}

	public abstract String generateComponent();
}
