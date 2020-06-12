package dev.hephaestus.landmark.impl.names.provider.types;

import dev.hephaestus.landmark.impl.names.provider.NameComponentProvider;

public class Literal extends NameComponentProvider {
	private final String value;

	public Literal(String value, NameComponentProvider parent) {
		super(parent);
		this.value = value;
	}

	@Override
	public String generateComponent() {
		return this.value;
	}
}
