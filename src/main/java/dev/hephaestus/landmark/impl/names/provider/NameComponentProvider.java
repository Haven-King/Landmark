package dev.hephaestus.landmark.impl.names.provider;

import java.util.ArrayList;

import net.minecraft.text.MutableText;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public abstract class NameComponentProvider {
	private final Identifier id;
	private final ArrayList<NameComponentProvider> reusableComponents;

	protected TextColor color = TextColor.fromFormatting(Formatting.WHITE);

	public NameComponentProvider(Identifier id) {
		this.id = id;
		this.reusableComponents = new ArrayList<>();
	}

	public NameComponentProvider(NameComponentProvider parent) {
		this.id = parent.id;
		this.reusableComponents = parent.reusableComponents;
		this.color = parent.color;
	}

	public void addReusableComponent(NameComponentProvider provider) {
		this.reusableComponents.add(provider);
	}

	public Identifier getId() {
		return this.id;
	}

	public NameComponentProvider withColor(TextColor color) {
		this.color = color;
		return this;
	}

	public MutableText generateComponent(int reusableComponentIndex) {
		if ((reusableComponentIndex > this.reusableComponents.size() || reusableComponentIndex < 0)) {
			throw new IllegalArgumentException("Invalid reusableComponent number \"" + reusableComponentIndex + "\" in " + this.id.toString());
		}

		return this.reusableComponents.get(reusableComponentIndex).generateComponent();
	}

	public abstract MutableText generateComponent();

	public TextColor getColor() {
		return this.color;
	}
}
