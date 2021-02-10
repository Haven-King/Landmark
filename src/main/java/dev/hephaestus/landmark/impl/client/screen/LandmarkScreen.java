package dev.hephaestus.landmark.impl.client.screen;

import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

public abstract class LandmarkScreen extends Screen {
    protected LandmarkScreen() {
        super(LiteralText.EMPTY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
        this.renderElements(matrices, mouseX, mouseY, delta);
    }

    private void renderElements(MatrixStack matrixStack, int mouseX, int mouseY, float delta) {
        for (Element element : this.children) {
            if (element instanceof Drawable) {
                ((Drawable) element).render(matrixStack, mouseX, mouseY, delta);
            }
        }
    }
}
