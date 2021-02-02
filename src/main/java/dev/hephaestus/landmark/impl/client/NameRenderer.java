package dev.hephaestus.landmark.impl.client;

import static dev.hephaestus.landmark.impl.LandmarkClient.CONFIG;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.network.PacketContext;

@Environment(EnvType.CLIENT)
public class NameRenderer extends DrawableHelper {
	private static Text NAME;

	private static int NAME_DISPLAY_TOTAL_TICKS = 0;

	private static int duration() {
		return (int) (CONFIG.namePopupFadeIn + CONFIG.namePopupDuration + CONFIG.namePopupFadeOut);
	}

	public static void draw(MatrixStack matrices) {
		if (NAME != null && NAME_DISPLAY_TOTAL_TICKS > 0) {
			TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

			int alpha = 255;

			if (NAME_DISPLAY_TOTAL_TICKS >= (CONFIG.namePopupDuration + CONFIG.namePopupFadeOut) * 20) {
				float progress = (NAME_DISPLAY_TOTAL_TICKS - (CONFIG.namePopupDuration - CONFIG.namePopupFadeOut) * 20) / (CONFIG.namePopupFadeIn * 20);
				alpha = 255 - (int) (255 * progress);
			}

			if (NAME_DISPLAY_TOTAL_TICKS <= CONFIG.namePopupFadeOut * 20) {
				float progress = (NAME_DISPLAY_TOTAL_TICKS) / (CONFIG.namePopupFadeOut * 20);
				alpha = (int) (255 * progress);
			}

			matrices.push();

			int width = MinecraftClient.getInstance().getWindow().getScaledWidth();
			int height = MinecraftClient.getInstance().getWindow().getScaledHeight();
			float textWidth = textRenderer.getWidth(NAME) * 2F * CONFIG.namePopupScale;

			switch (CONFIG.namePopupRenderLocation) {
			case LEFT:
				matrices.translate(CONFIG.namePopupOffset, CONFIG.namePopupOffset, 0);
				break;
			case CENTER:
				matrices.translate(width / 2F - textWidth / 2F, height / 4F, 0);
				break;
			case RIGHT:
				matrices.translate(width - textWidth - CONFIG.namePopupOffset, CONFIG.namePopupOffset, 0);
				break;
			}

			matrices.scale(2.F * CONFIG.namePopupScale, 2F * CONFIG.namePopupScale, 1);
			textRenderer.drawWithShadow(matrices, NAME, 0, 0, alpha << 24 | 0x00FFFFFF);

			matrices.pop();
		}

		--NAME_DISPLAY_TOTAL_TICKS;
	}

	@Environment(EnvType.CLIENT)
	public static void accept(MinecraftClient client, ClientPlayNetworkHandler network, PacketByteBuf buf, PacketSender sender) {
		Text landmarkName = buf.readText();

		if (NAME_DISPLAY_TOTAL_TICKS <= 0) {
			NAME_DISPLAY_TOTAL_TICKS = duration() * 20;
			NAME = landmarkName;
		}
	}

	public enum RenderLocation {
		LEFT, CENTER, RIGHT
	}
}
