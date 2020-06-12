package dev.hephaestus.landmark.impl.names.provider;

import java.util.ArrayList;

import net.minecraft.util.Identifier;

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
