package dev.hephaestus.landmark.impl.names.provider.types;

import dev.hephaestus.landmark.impl.names.provider.NameComponentProvider;

import net.minecraft.text.MutableText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;

public class Translatable extends NameComponentProvider {
	private final String value;

	public Translatable(Identifier id, String value) {
		super(id);
		this.value = value;
	}

	@Override
	public MutableText generateComponent() {
		return new TranslatableText(this.value);
	}
}
