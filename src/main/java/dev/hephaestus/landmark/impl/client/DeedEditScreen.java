package dev.hephaestus.landmark.impl.client;

import dev.hephaestus.landmark.impl.item.DeedItem;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.fabricmc.fabric.api.network.PacketContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

import java.util.UUID;

public class DeedEditScreen extends Screen {
    private final Text text;
    private final UUID deedId;
    private final Hand hand;

    private TextColor textColor;
    private TextFieldWidget nameField;
    private TextFieldWidget colorField;
//    private ButtonWidget finalizeButton;
    private ButtonWidget saveButton;

    public DeedEditScreen(UUID deedId, ItemStack stack, Hand hand) {
        super(new TranslatableText("deed.landmark.finalize"));
        Text text;

        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("deed_name")) {
            text = Text.Serializer.fromJson(tag.getString("deed_name"));
            this.text = text == null ? new LiteralText("") : text;
            this.textColor = text == null || text.getStyle() == null ? TextColor.fromFormatting(Formatting.WHITE) : text.getStyle().getColor();
        } else {
            this.text = new LiteralText("");
            this.textColor = TextColor.fromFormatting(Formatting.WHITE);
        }

        this.deedId = deedId;


        this.hand = hand;
    }

    @Override
    public void init(MinecraftClient client, int width, int height) {
        if (this.deedId != null) {
            this.client = client;
            this.client.keyboard.enableRepeatEvents(true);
            this.nameField = new TextFieldWidget(this.client.textRenderer, width / 4, height / 2 - 10, width / 2, 20, new LiteralText(""));
            this.nameField.setText(this.text.asString());
            this.nameField.setEditableColor(this.textColor.getRgb());

//            int subWidth = (width / 4) - 2;

            this.colorField = new TextFieldWidget(this.client.textRenderer, (3 * width / 4) + 4, height / 2 - 10, width / 6, 20, new LiteralText(""));

            int color = this.text.getStyle().getColor() == null ? 0xFFFFFF : this.text.getStyle().getColor().getRgb();
            this.colorField.setText("#" + Integer.toHexString(color));
            this.colorField.setEditableColor(this.textColor.getRgb());
            this.colorField.setChangedListener((string) -> {
                this.textColor = TextColor.parse(this.colorField.getText().toLowerCase());
                this.textColor = this.textColor == null ? TextColor.fromFormatting(Formatting.WHITE) : textColor;

                if (this.textColor != null) {
                    this.nameField.setEditableColor(this.textColor.getRgb());
                    this.colorField.setEditableColor(this.textColor.getRgb());
                }
            });

            this.saveButton = new ButtonWidget(width / 4, height / 2 + 14, width / 2, 20, new TranslatableText("deeds.landmark.save"), action -> {
                PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
                buf.writeUuid(this.deedId);
                buf.writeText(new LiteralText(nameField.getText()).styled((style) -> style.withColor(this.textColor)));
                buf.writeEnumConstant(this.hand);
                ClientSidePacketRegistry.INSTANCE.sendToServer(DeedItem.DEED_SAVE_PACKET_ID, buf);
                client.openScreen(null);
            });

//            this.finalizeButton = new ButtonWidget(width / 4 + subWidth + 4, height / 2 + 14, subWidth, 20, new TranslatableText("deeds.landmark.finalize"), action -> {
//                PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
//                buf.writeUuid(this.deedId);
//                buf.writeText(new LiteralText(nameField.getText()).styled((style) -> style.withColor(this.textColor)));
//                buf.writeEnumConstant(this.hand);
//                ClientSidePacketRegistry.INSTANCE.sendToServer(DeedItem.DEED_FINALIZE_PACKET_ID, buf);
//                client.openScreen(null);
//            });

            this.children.add(this.nameField);
            this.children.add(this.colorField);
            this.children.add(this.saveButton);
//            this.children.add(this.finalizeButton);
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        DrawableHelper.fill(matrices, 0, 0, 1000, 1000, 0x88000000);
        this.nameField.render(matrices, mouseX, mouseY, delta);
        this.colorField.render(matrices, mouseX, mouseY, delta);
        this.saveButton.render(matrices, mouseX, mouseY, delta);
//        this.finalizeButton.render(matrices, mouseX, mouseY, delta);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public static void open(PacketContext context, PacketByteBuf packetByteBuf) {
        PlayerEntity playerEntity = context.getPlayer();
        Hand hand = packetByteBuf.readEnumConstant(Hand.class);
        UUID deedId = packetByteBuf.readUuid();

        context.getTaskQueue().execute(() -> MinecraftClient.getInstance().openScreen(new DeedEditScreen(deedId, playerEntity.getStackInHand(hand), hand)));
    }
}
