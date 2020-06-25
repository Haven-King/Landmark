package dev.hephaestus.landmark.impl.client.gui;

import java.util.UUID;

import dev.hephaestus.landmark.impl.network.LandmarkNetworking;
import io.netty.buffer.Unpooled;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.fabricmc.fabric.api.network.PacketContext;

public class EditScreen extends LandmarkScreen {
	private final Text text;
	private final UUID deedId;
	private final Hand hand;

	private TextColor textColor;

	public EditScreen(PacketContext context, PacketByteBuf buf) {
		super(context, buf);

		PlayerEntity playerEntity = context.getPlayer();
		this.hand = buf.readEnumConstant(Hand.class);
		this.deedId = buf.readUuid();

		CompoundTag tag = playerEntity.getStackInHand(hand).getTag();

		if (tag != null && tag.contains("landmark_name")) {
			Text text = Text.Serializer.fromJson(tag.getString("landmark_name"));
			this.text = text == null ? new LiteralText("") : text;
			this.textColor = text == null || text.getStyle() == null ? TextColor.fromFormatting(Formatting.WHITE) : text.getStyle().getColor();
		} else {
			this.text = new LiteralText("");
			this.textColor = TextColor.fromFormatting(Formatting.WHITE);
		}

		context.getTaskQueue().execute(() -> MinecraftClient.getInstance().openScreen(this));
	}

	@Override
	public void init(MinecraftClient client, int width, int height) {
		super.init(client, width, height);

		if (this.deedId != null) {
			this.client = client;
			this.client.keyboard.enableRepeatEvents(true);
			TextFieldWidget nameField = new TextFieldWidget(this.client.textRenderer, width / 4, height / 2 - 10, width / 2, 20, new LiteralText(""));
			nameField.setText(this.text.asString());
			nameField.setEditableColor(this.textColor.getRgb());
			this.children.add(nameField);

			TextFieldWidget colorField = new TextFieldWidget(this.client.textRenderer, (3 * width / 4) + 4, height / 2 - 10, width / 6, 20, new LiteralText(""));
			int color = this.text.getStyle().getColor() == null ? 0xFFFFFF : this.text.getStyle().getColor().getRgb();
			colorField.setText("#" + Integer.toHexString(color));
			colorField.setEditableColor(this.textColor.getRgb());
			colorField.setChangedListener((string) -> {
				this.textColor = TextColor.parse(colorField.getText().toLowerCase());
				this.textColor = this.textColor == null ? TextColor.fromFormatting(Formatting.WHITE) : textColor;

				if (this.textColor != null) {
					nameField.setEditableColor(this.textColor.getRgb());
					colorField.setEditableColor(this.textColor.getRgb());
				}
			});
			this.children.add(colorField);

			ButtonWidget deleteButton = new ButtonWidget(width / 4, height / 2 + 14, width / 4 - 2, 20, new TranslatableText("deeds.landmark.delete").styled(style -> style.withColor(Formatting.RED)), action -> {
				PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
				buf.writeUuid(this.deedId);
				buf.writeBoolean(true);
				buf.writeEnumConstant(this.hand);
				ClientSidePacketRegistry.INSTANCE.sendToServer(LandmarkNetworking.TRACKER_DELETE_LANDMARK, buf);
				this.client.openScreen(null);
			});
			this.children.add(deleteButton);

			ButtonWidget saveButton = new ButtonWidget(width / 2 + 2, height / 2 + 14, width / 4 - 2, 20, new TranslatableText("deeds.landmark.save"), action -> {
				PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
				buf.writeUuid(this.deedId);
				buf.writeText(new LiteralText(nameField.getText()).styled((style) -> style.withColor(this.textColor)));
				buf.writeEnumConstant(this.hand);
				ClientSidePacketRegistry.INSTANCE.sendToServer(LandmarkNetworking.SAVE_LANDMARK_NAME, buf);
				client.openScreen(null);
			});
			this.children.add(saveButton);
		}
	}
}
