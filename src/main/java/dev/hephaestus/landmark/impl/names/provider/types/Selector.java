package dev.hephaestus.landmark.impl.names.provider.types;

import java.util.ArrayList;

import dev.hephaestus.landmark.impl.names.provider.MultiComponentProvider;
import dev.hephaestus.landmark.impl.names.provider.NameComponentProvider;

import net.minecraft.text.MutableText;
import net.minecraft.util.Identifier;

public class Selector extends MultiComponentProvider {
	private final ArrayList<NameComponentProvider> components = new ArrayList<>();

	public Selector(Identifier id) {
		super(id);
	}

	public Selector(NameComponentProvider parent) {
		super(parent);
	}

	@Override
	public void addComponent(NameComponentProvider provider) {
		this.components.add(provider);
	}

	@Override
	public MutableText generateComponent() {
		return this.components.get((int) (Math.random() * components.size())).generateComponent();
	}
}
