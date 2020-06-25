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
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.fabricmc.fabric.api.network.PacketContext;

public class ClaimScreen extends LandmarkScreen {
	private final Hand hand;

	public ClaimScreen(PacketContext context, PacketByteBuf buf) {
		super(context, buf);
		this.hand = buf.readEnumConstant(Hand.class);
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

					if (landmark.canModify(this.client.player)) {
						int finalI = i;

						ButtonWidget widget = new LandmarkButtonWidget(width / 2 - 100, width / 10 + 24 * i, 200, 20, landmark, (action) -> {
							PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
							buf.writeUuid(sections.get(finalI));
							buf.writeEnumConstant(this.hand);
							ClientSidePacketRegistry.INSTANCE.sendToServer(LandmarkNetworking.TRACKER_CLAIM_LANDMARK, buf);
							client.openScreen(null);
						});

						this.addButton(widget);
					}
				}

				ButtonWidget newLandmark = new ButtonWidget(width / 2 - 100, height / 10 + 24 * (this.buttons.size() + 1), 200, 20, new TranslatableText("deeds.landmark.create").styled(style -> style.withColor(Formatting.GREEN)), (action) -> {
					PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
					buf.writeEnumConstant(this.hand);
					ClientSidePacketRegistry.INSTANCE.sendToServer(LandmarkNetworking.TRACKER_NEW_LANDMARK, buf);
				});

				this.addButton(newLandmark);
				this.addChild(new TextWidget(this.textRenderer, new TranslatableText("deeds.landmark.claim"), this.width / 2F - 100, this.width / 10F - 20));
			}
		}
	}
}
