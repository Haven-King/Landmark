package dev.hephaestus.landmark.impl.client;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicReferenceArray;

import dev.hephaestus.landmark.impl.LandmarkMod;
import dev.hephaestus.landmark.impl.item.DeedItem;
import dev.hephaestus.landmark.impl.item.EvictionNoticeItem;
import dev.hephaestus.landmark.impl.landmarks.LandmarkSection;
import dev.hephaestus.landmark.impl.world.chunk.LandmarkChunkComponent;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public class DeedBuilderRenderer {
	@Environment(EnvType.CLIENT)
	public static void render(BufferBuilderStorage bufferBuilders, MatrixStack matrices, ItemStack renderStack, Vec3d camPos) {
		if (renderStack.getItem() instanceof DeedItem || renderStack.getItem() instanceof EvictionNoticeItem) {
			matrices.push();
			matrices.translate(-camPos.x, -camPos.y, -camPos.z);
			VertexConsumer vertexConsumer = bufferBuilders.getEffectVertexConsumers().getBuffer(RenderLayer.getLines());

			AtomicReferenceArray<WorldChunk> chunks = MinecraftClient.getInstance().world.getChunkManager().chunks.chunks;

			HashSet<LandmarkSection> sections = new HashSet<>();

			for (int i = 0; i < chunks.length(); ++i) {
				if (chunks.get(i) != null) {
					LandmarkChunkComponent component = LandmarkMod.CHUNK_COMPONENT.get(chunks.get(i));
					sections.addAll(component.getSections());
				}
			}

			for (LandmarkSection section : sections) {
				section.render(matrices, vertexConsumer);
			}

			matrices.pop();
		}
	}
}
