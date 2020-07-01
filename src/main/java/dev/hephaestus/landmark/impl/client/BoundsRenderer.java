package dev.hephaestus.landmark.impl.client;

import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReferenceArray;

import dev.hephaestus.landmark.impl.LandmarkMod;
import dev.hephaestus.landmark.impl.item.DeedItem;
import dev.hephaestus.landmark.impl.item.EvictionNoticeItem;
import dev.hephaestus.landmark.impl.landmarks.Landmark;
import dev.hephaestus.landmark.impl.landmarks.LandmarkSection;
import dev.hephaestus.landmark.impl.world.LandmarkTrackingComponent;
import dev.hephaestus.landmark.impl.world.chunk.LandmarkChunkComponent;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class BoundsRenderer {
	public static void render(BufferBuilderStorage bufferBuilders, MatrixStack matrices, ItemStack renderStack, Vec3d camPos) {
		if (renderStack.getItem() instanceof DeedItem || renderStack.getItem() instanceof EvictionNoticeItem) {
			matrices.push();
			matrices.translate(-camPos.x, -camPos.y, -camPos.z);
			VertexConsumer linesVertexConsumer = bufferBuilders.getEffectVertexConsumers().getBuffer(RenderLayer.getLines());

			if (MinecraftClient.getInstance().world != null) {
				AtomicReferenceArray<WorldChunk> chunks = MinecraftClient.getInstance().world.getChunkManager().chunks.chunks;

				HashSet<LandmarkSection> sections = new HashSet<>();

				for (int i = 0; i < chunks.length(); ++i) {
					if (chunks.get(i) != null) {
						LandmarkChunkComponent component = LandmarkMod.CHUNK_COMPONENT.get(chunks.get(i));
						sections.addAll(component.getSections());
					}
				}

				CompoundTag tag = renderStack.getOrCreateTag();
				UUID selected = null;

				if (tag.containsUuid("landmark_id")) {
					selected = tag.getUuid("landmark_id");
				}

				for (LandmarkSection section : sections) {
					section.render(matrices, linesVertexConsumer, selected);
				}

				if (selected != null) {
					Vector3f color;

					// if (tag.contains("delete_mode") && tag.getBoolean("delete_mode")) {
					//   color = Vector3f.POSITIVE_X;
					// } else {
					Landmark landmark = LandmarkTrackingComponent.of(MinecraftClient.getInstance().world).get(selected);
					color = landmark == null ? new Vector3f(1F, 1F, 1F) : landmark.getColor();
					// }

					if (tag.contains("marker")) {
						BlockPos pos1 = BlockPos.fromLong(tag.getLong("marker"));
						BlockPos pos2;
						HitResult result = MinecraftClient.getInstance().crosshairTarget;

						if (result instanceof BlockHitResult) {
							pos2 = ((BlockHitResult) result).getBlockPos();
						} else {
							pos2 = pos1;
						}

						BlockBox box = new BlockBox(pos1, pos2);

						WorldRenderer.drawBox(matrices, linesVertexConsumer, box.minX, box.minY, box.minZ, box.maxX + 1, box.maxY + 1, box.maxZ + 1, color.getX(), color.getY(), color.getZ(), 1);
					} else if (MinecraftClient.getInstance().player != null) {
						HitResult result = MinecraftClient.getInstance().crosshairTarget;

						if (result instanceof BlockHitResult) {
							BlockPos pos = ((BlockHitResult) result).getBlockPos();
							WorldRenderer.drawBox(matrices, linesVertexConsumer, pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1, color.getX(), color.getY(), color.getZ(), 1);
						}
					}
				}
			}

			matrices.pop();
		}
	}
}
