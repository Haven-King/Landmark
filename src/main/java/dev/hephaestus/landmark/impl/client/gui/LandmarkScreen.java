package dev.hephaestus.landmark.impl.client.gui;

import net.fabricmc.fabric.api.network.PacketContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.LiteralText;

public abstract class LandmarkScreen extends Screen {
	protected LandmarkScreen(PacketContext context, PacketByteBuf buf) {
		super(LiteralText.EMPTY);
	}

	@Override
	public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
		fill(matrices, 0, 0, this.width, this.height, 0x88000000);
		for (Element element : this.children) {
			if (element instanceof Drawable) {
				((Drawable) element).render(matrices, mouseX, mouseY, delta);
			}
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
