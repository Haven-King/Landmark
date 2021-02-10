package dev.hephaestus.landmark.impl.client.screen;

import dev.hephaestus.landmark.api.LandmarkCache;
import dev.hephaestus.landmark.impl.LandmarkNetworking;
import dev.hephaestus.landmark.impl.util.Landmark;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

import java.util.ArrayList;
import java.util.Collection;

public class ClaimScreen extends LandmarkScreen {
    private static final Text CREATE_NEW = new TranslatableText("gui.landmark.new");

    private final ClientPlayerEntity player;
    private final Collection<Landmark> landmarks;
    private final int slot;

    public ClaimScreen(ClientPlayerEntity player, int slot) {
        super();
        this.player = player;
        this.landmarks = new ArrayList<>();

        for (Landmark landmark : LandmarkCache.getInstance(player.world).getLandmarks(player.getBlockPos())) {
            if (landmark.isOwnedBy(player) || !landmark.isClaimed()) {
                this.landmarks.add(landmark);
            }
        }

        this.slot = slot;
    }

    @Override
    public void init(MinecraftClient client, int width, int height) {
        super.init(client, width, height);

        if (client == null) return;

        TextRenderer textRenderer = client.textRenderer;

        int buttonWidth = textRenderer.getWidth(CREATE_NEW);

        for (Landmark landmark : this.landmarks) {
            buttonWidth = Math.max(buttonWidth, textRenderer.getWidth(landmark.getName()));
        }

        buttonWidth += 10;

        int startX = width / 2 - buttonWidth / 2;
        int startY = height / 2 - 10 * this.landmarks.size() - 5;

        for (Landmark landmark : this.landmarks) {
            Text text = landmark.getName().copy().setStyle(Style.EMPTY);
            this.addButton(new ButtonWidget(startX, startY + 20 * this.children.size(), buttonWidth, 20, text, button -> {
                LandmarkNetworking.claimLandmark(landmark.getId(), this.slot);
            }));
        }

        int createY = this.landmarks.size() > 0
                ? height / 2 + 10 * this.landmarks.size() + 5
                : height / 2 - 10;

        this.addButton(new ButtonWidget(startX, createY, buttonWidth, 20, CREATE_NEW, button -> {
            LandmarkNetworking.newLandmark(this.slot);
        }));
    }
}
