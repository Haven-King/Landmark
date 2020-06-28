package dev.hephaestus.landmark.impl.client.gui;

import java.util.List;
import java.util.UUID;

import dev.hephaestus.landmark.impl.client.gui.widget.LandmarkButtonWidget;
import dev.hephaestus.landmark.impl.client.gui.widget.TextWidget;
import dev.hephaestus.landmark.impl.landmarks.Landmark;
import dev.hephaestus.landmark.impl.network.LandmarkNetworking;
import dev.hephaestus.landmark.impl.world.LandmarkTrackingComponent;
import dev.hephaestus.landmark.impl.world.chunk.LandmarkChunkComponent;
import io.netty.buffer.Unpooled;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.TranslatableText;

import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.fabricmc.fabric.api.network.PacketContext;

public class DeletionScreen extends LandmarkScreen {
	public DeletionScreen(PacketContext context, PacketByteBuf buf) {
		super(context, buf);
		context.getTaskQueue().execute(() -> MinecraftClient.getInstance().openScreen(this));
	}

	@Override
	public void init(MinecraftClient client, int width, int height) {
		super.init(client, width, height);

		if (this.client != null) {
			LandmarkTrackingComponent tracker = LandmarkTrackingComponent.of(this.client.world);

			if (this.client.world != null && this.client.player != null) {
				List<UUID> sections = LandmarkChunkComponent.of(this.client.world.getChunk(this.client.player.chunkX, this.client.player.chunkZ)).getIds();

				for (int i = 0; i < sections.size(); ++i) {
					Landmark landmark = tracker.get(sections.get(i));
					if (landmark != null && landmark.canModify(this.client.player)) {
						int finalI = i;
						ButtonWidget widget = new LandmarkButtonWidget(width / 2 - 100, width / 10 + 24 * this.buttons.size(), 200, 20, landmark, (action) -> {
							PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
							buf.writeUuid(sections.get(finalI));
							buf.writeBoolean(false);
							ClientSidePacketRegistry.INSTANCE.sendToServer(LandmarkNetworking.TRACKER_DELETE_LANDMARK, buf);
							client.openScreen(null);
						});

						this.addButton(widget);
					}
				}
			}

			this.addChild(new TextWidget(this.textRenderer, new TranslatableText("deeds.landmark.delete.message"), this.width / 2F - 100, this.width / 10F - 20));
		}
	}
}
