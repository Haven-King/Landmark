package dev.hephaestus.landmark.impl.client;

import dev.hephaestus.landmark.impl.landmarks.Landmark;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.util.Formatting;

public class LandmarkButtonWidget extends ButtonWidget {

	public LandmarkButtonWidget(int x, int y, int width, int height, Landmark landmark, PressAction onPress) {
		super(x, y, width, height, landmark.getName().styled(style -> style.withColor(
				landmark.canModify(MinecraftClient.getInstance().player) ? Formatting.WHITE : Formatting.RED
		)), onPress);
	}
}
