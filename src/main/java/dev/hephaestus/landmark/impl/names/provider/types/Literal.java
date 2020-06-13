package dev.hephaestus.landmark.impl.names.provider.types;

import dev.hephaestus.landmark.impl.names.provider.NameComponentProvider;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;

public class Literal extends NameComponentProvider {
	private final String value;

	public Literal(String value, NameComponentProvider parent) {
		super(parent);
		this.value = value;
	}

	@Override
	public MutableText generateComponent() {
		return new LiteralText(this.value);
	}
}
