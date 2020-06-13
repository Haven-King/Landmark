package dev.hephaestus.landmark.impl.names.provider.types;

import java.util.ArrayList;

import dev.hephaestus.landmark.impl.names.provider.MultiComponentProvider;
import dev.hephaestus.landmark.impl.names.provider.NameComponentProvider;

import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.util.Identifier;

public class Collector extends MultiComponentProvider {
	private final ArrayList<NameComponentProvider> components = new ArrayList<>();

	public Collector(Identifier id) {
		super(id);
	}

	public Collector(NameComponentProvider parent) {
		super(parent);
	}

	@Override
	public void addComponent(NameComponentProvider provider) {
		this.components.add(provider);
	}

	@Override
	public MutableText generateComponent() {
		MutableText result = new LiteralText("");

		for (NameComponentProvider provider : this.components) {
			result.append(provider.generateComponent());
		}

		return result;
	}
}
