package dev.hephaestus.landmark.impl.names.provider;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.Identifier;

public abstract class MultiComponentProvider extends NameComponentProvider {
	public MultiComponentProvider(Identifier id) {
		super(id);
	}

	public MultiComponentProvider(NameComponentProvider parent) {
		super(parent);
	}

	public abstract void addComponent(NameComponentProvider provider);

}
