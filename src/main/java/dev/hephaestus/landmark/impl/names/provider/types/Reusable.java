package dev.hephaestus.landmark.impl.names.provider.types;

import dev.hephaestus.landmark.impl.names.provider.NameComponentProvider;

public class Reusable extends NameComponentProvider {
	private final int reusableComponentIndex;

	public Reusable(int reusableComponentIndex, NameComponentProvider parent) {
		super(parent);
		this.reusableComponentIndex = reusableComponentIndex;
	}

	@Override
	public String generateComponent() {
		return generateComponent(this.reusableComponentIndex);
	}
}
