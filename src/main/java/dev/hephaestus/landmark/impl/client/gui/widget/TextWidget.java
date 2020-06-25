package dev.hephaestus.landmark.impl.client.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class TextWidget implements Drawable, Element {
	private final TextRenderer textRenderer;
	private final Text text;
	private final float x;
	private final float y;
	private final int color;


	public TextWidget(TextRenderer textRenderer, Text text, float x, float y, int color) {
		this.textRenderer = textRenderer;
		this.text = text;
		this.x = x;
		this.y = y;
		this.color = color;
	}

	public TextWidget(TextRenderer textRenderer, Text text, float x, float y) {
		this(textRenderer, text, x, y, 0xFFFFFF);
	}

	@Override
	public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
		this.textRenderer.drawWithShadow(matrices, this.text, this.x, this.y, this.color);
	}
}
