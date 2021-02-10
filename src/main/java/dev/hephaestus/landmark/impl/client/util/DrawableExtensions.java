package dev.hephaestus.landmark.impl.client.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.Matrix4f;

import static org.lwjgl.opengl.GL11.GL_LINES;

public interface DrawableExtensions {
	default void line(MatrixStack matrices, float x0, float x1, float y0, float y1, int color) {
		Matrix4f matrix = matrices.peek().getModel();

		float a = (float)(color >> 24 & 255) / 255.0F;
		float r = (float)(color >> 16 & 255) / 255.0F;
		float g = (float)(color >> 8 & 255) / 255.0F;
		float b = (float)(color & 255) / 255.0F;
		BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
		RenderSystem.enableBlend();
		RenderSystem.disableTexture();
		RenderSystem.defaultBlendFunc();

		bufferBuilder.begin(1, VertexFormats.POSITION_COLOR);
		bufferBuilder.vertex(matrix, x0, y0, 0F).color(r, g, b, a).next();
		bufferBuilder.vertex(matrix, x1, y1, 0F).color(r, g, b, a).next();
		bufferBuilder.end();

		BufferRenderer.draw(bufferBuilder);
		RenderSystem.enableTexture();
		RenderSystem.disableBlend();
	}

	default void drawCenteredText(MatrixStack matrices, TextRenderer textRenderer, Text text, float centerX, float y, int color, float scale) {
		matrices.push();
		matrices.scale(scale, scale, 1F);
		matrices.translate(centerX / scale, (y + textRenderer.fontHeight / 2F) / scale, 0);
		DrawableHelper.drawCenteredText(matrices, textRenderer, text, 0, -textRenderer.fontHeight / 2, color);

		matrices.pop();
	}

	default void drawCenteredString(MatrixStack matrices, TextRenderer textRenderer, String string, float centerX, float y, int color, float scale) {
		matrices.push();
		matrices.scale(scale, scale, 1F);
		matrices.translate(centerX / scale, (y + textRenderer.fontHeight / 2F) / scale, 0);
		DrawableHelper.drawCenteredString(matrices, textRenderer, string, 0, -textRenderer.fontHeight / 2, color);
		matrices.pop();
	}

	default void draw(MatrixStack matrices, TextRenderer textRenderer, Text text, float x, float y, int color, float scale) {
		matrices.push();
		matrices.scale(scale, scale, 1F);
		matrices.translate(x / scale, (y + textRenderer.fontHeight / 2F) / scale, 0);
		textRenderer.draw(matrices, text, 0, -textRenderer.fontHeight / 2F, color);
		matrices.pop();
	}

	default void draw(MatrixStack matrices, TextRenderer textRenderer, OrderedText text, int x, int y, int color, float scale) {
		matrices.push();
		matrices.scale(scale, scale, 1F);
		matrices.translate(x / scale, (y + textRenderer.fontHeight / 2F) / scale, 0);
		textRenderer.draw(matrices, text, 0, -textRenderer.fontHeight / 2F, color);
		matrices.pop();
	}

	default void draw(MatrixStack matrices, TextRenderer textRenderer, String text, float x, float y, int color, float scale) {
		matrices.push();
		matrices.scale(scale, scale, 1F);
		matrices.translate(x / scale, (y + textRenderer.fontHeight / 2F) / scale, 0);
		textRenderer.draw(matrices, text, 0, -textRenderer.fontHeight / 2F, color);
		matrices.pop();
	}

	default void fill(Matrix4f matrix, BufferBuilder bufferBuilder, float xStart, float yStart, float xEnd, float yEnd, int z, int color) {
		float f = (float)(color >> 24 & 255) / 255.0F;
		float g = (float)(color >> 16 & 255) / 255.0F;
		float h = (float)(color >> 8 & 255) / 255.0F;
		float i = (float)(color & 255) / 255.0F;
		bufferBuilder.vertex(matrix, xEnd, yStart, (float)z).color(g, h, i, f).next();
		bufferBuilder.vertex(matrix, xStart, yStart, (float)z).color(g, h, i, f).next();
		bufferBuilder.vertex(matrix, xStart, yEnd, (float)z).color(g, h, i, f).next();
		bufferBuilder.vertex(matrix, xEnd, yEnd, (float)z).color(g, h, i, f).next();
	}

	default void fill(MatrixStack matrices, float x1, float y1, float x2, float y2, int color, float alpha) {
		Matrix4f matrix = matrices.peek().getModel();

		float j;
		if (x1 < x2) {
			j = x1;
			x1 = x2;
			x2 = j;
		}

		if (y1 < y2) {
			j = y1;
			y1 = y2;
			y2 = j;
		}

		float r = (float)(color >> 16 & 255) / 255.0F;
		float g = (float)(color >> 8 & 255) / 255.0F;
		float b = (float)(color & 255) / 255.0F;
		BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
		RenderSystem.enableBlend();
		RenderSystem.disableTexture();
		RenderSystem.defaultBlendFunc();

		bufferBuilder.begin(7, VertexFormats.POSITION_COLOR);

		bufferBuilder.vertex(matrix, x1, y2, 0.0F).color(r, g, b, alpha).next();
		bufferBuilder.vertex(matrix, x2, y2, 0.0F).color(r, g, b, alpha).next();
		bufferBuilder.vertex(matrix, x2, y1, 0.0F).color(r, g, b, alpha).next();
		bufferBuilder.vertex(matrix, x1, y1, 0.0F).color(r, g, b, alpha).next();

		bufferBuilder.end();
		BufferRenderer.draw(bufferBuilder);
		RenderSystem.enableTexture();
		RenderSystem.disableBlend();
	}

	default void box(MatrixStack matrices, float x1, float y1, float x2, float y2, int color, float alpha, float lineWidth) {
		Matrix4f matrix = matrices.peek().getModel();

		float j;
		if (x1 < x2) {
			j = x1;
			x1 = x2;
			x2 = j;
		}

		if (y1 < y2) {
			j = y1;
			y1 = y2;
			y2 = j;
		}

		float r = (float)(color >> 16 & 255) / 255.0F;
		float g = (float)(color >> 8 & 255) / 255.0F;
		float b = (float)(color & 255) / 255.0F;
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
		RenderSystem.enableBlend();
		RenderSystem.disableTexture();
		RenderSystem.defaultBlendFunc();

		bufferBuilder.begin(GL_LINES, VertexFormats.POSITION_COLOR);

		bufferBuilder.vertex(matrix, x1, y2, 0.0F).color(r, g, b, alpha).next();
		bufferBuilder.vertex(matrix, x2, y2, 0.0F).color(r, g, b, alpha).next();
		bufferBuilder.vertex(matrix, x2, y1, 0.0F).color(r, g, b, alpha).next();
		bufferBuilder.vertex(matrix, x1, y1, 0.0F).color(r, g, b, alpha).next();

		bufferBuilder.vertex(matrix, x1, y2, 0.0F).color(r, g, b, alpha).next();
		bufferBuilder.vertex(matrix, x1, y1, 0.0F).color(r, g, b, alpha).next();
		bufferBuilder.vertex(matrix, x2, y2, 0.0F).color(r, g, b, alpha).next();
		bufferBuilder.vertex(matrix, x2, y1, 0.0F).color(r, g, b, alpha).next();

		tessellator.draw();
		RenderSystem.enableTexture();
		RenderSystem.disableBlend();
	}

	default int darken(int color, double amount) {
		int a = (int) ((color & 0xFF000000) * amount);
		int r = (int) ((color & 0x00FF0000) * amount);
		int g = (int) ((color & 0x0000FF00) * amount);
		int b = (int) ((color & 0x000000FF) * amount);

		return a | r | g | b;
	}
}
