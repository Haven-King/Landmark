package dev.hephaestus.landmark.impl.client.screen;

import dev.hephaestus.landmark.api.LandmarkCache;
import dev.hephaestus.landmark.impl.LandmarkNetworking;
import dev.hephaestus.landmark.impl.util.Landmark;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.hud.BackgroundHelper;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.function.Predicate;

public class EditScreen extends LandmarkScreen {
    private static final Predicate<String> COLOR_PREDICATE = string ->
            string.isEmpty() || string.matches("\\b(1?[0-9]{1,2}|2[0-4][0-9]|25[0-5])\\b");

    private static final Text CANCEL = new TranslatableText("gui.cancel");
    private static final Text DONE = new TranslatableText("gui.done");

    private final Landmark landmark;

    public EditScreen(ClientPlayerEntity player, int slot) {
        ItemStack stack = player.inventory.getStack(slot);

        this.landmark = LandmarkCache.getInstance(player.world).getLandmark(stack.getTag().getUuid("landmark_id"));
    }

    public EditScreen(ClientPlayerEntity player, Landmark landmark) {
        this.landmark = landmark;
    }

    @Override
    public void init(MinecraftClient client, int width, int height) {
        super.init(client, width, height);

        if (client == null || this.landmark == null) return;

        TextRenderer textRenderer = client.textRenderer;

        int entryWidth = 200;

        int x = width / 2 - entryWidth / 2;

        TextFieldWidget nameEntry = new TextFieldWidget(textRenderer, x, height / 2 - 15, entryWidth, 20, LiteralText.EMPTY);
        nameEntry.setText(this.landmark.getName().getString());

        Vector3f c = this.landmark.getColor();
        MutableInt r = new MutableInt(MathHelper.ceil(255 * c.getX()));
        MutableInt g = new MutableInt(MathHelper.ceil(255 * c.getY()));
        MutableInt b = new MutableInt(MathHelper.ceil(255 * c.getZ()));

        nameEntry.setEditableColor(BackgroundHelper.ColorMixer.getArgb(255, r.getValue(), g.getValue(), b.getValue()));
        this.addChild(nameEntry);

        int colorEntryWidth = entryWidth / 3 - 6;

        TextFieldWidget red = new TextFieldWidget(textRenderer, x, height / 2 + 15, colorEntryWidth, 20, LiteralText.EMPTY);
        red.setText(String.valueOf(r));
        red.setTextPredicate(COLOR_PREDICATE);
        red.setChangedListener(string -> {
            r.setValue(string.isEmpty() ? 0 : Integer.parseInt(string));
            nameEntry.setEditableColor(BackgroundHelper.ColorMixer.getArgb(255, r.getValue(), g.getValue(), b.getValue()));
        });

        this.addChild(red);

        TextFieldWidget green = new TextFieldWidget(textRenderer, x + entryWidth / 2 - colorEntryWidth /2, height / 2 + 15, colorEntryWidth, 20, LiteralText.EMPTY);
        green.setText(String.valueOf(g));
        green.setTextPredicate(COLOR_PREDICATE);
        green.setChangedListener(string -> {
            g.setValue(string.isEmpty() ? 0 : Integer.parseInt(string));
            nameEntry.setEditableColor(BackgroundHelper.ColorMixer.getArgb(255, r.getValue(), g.getValue(), b.getValue()));
        });

        this.addChild(green);

        TextFieldWidget blue = new TextFieldWidget(textRenderer, x + entryWidth - colorEntryWidth, height / 2 + 15, colorEntryWidth, 20, LiteralText.EMPTY);
        blue.setText(String.valueOf(b));
        blue.setTextPredicate(COLOR_PREDICATE);
        blue.setChangedListener(string -> {
            b.setValue(string.isEmpty() ? 0 : Integer.parseInt(string));
            nameEntry.setEditableColor(BackgroundHelper.ColorMixer.getArgb(255, r.getValue(), g.getValue(), b.getValue()));
        });

        this.addChild(blue);

        this.addButton(new ButtonWidget(x, height / 2 + 40, entryWidth / 2 - 5, 20, CANCEL, button -> {
            this.onClose();
        }));

        this.addButton(new ButtonWidget(x + entryWidth / 2 + 5, height / 2 + 40, entryWidth / 2 - 5, 20, DONE, button -> {
            Text name = new LiteralText(nameEntry.getText()).styled(style -> style.withColor(TextColor.fromRgb(
                    BackgroundHelper.ColorMixer.getArgb(0, r.getValue(), g.getValue(), b.getValue())
            )));

            LandmarkNetworking.setName(this.landmark.getId(), name);
            this.onClose();
        }));
    }
}
