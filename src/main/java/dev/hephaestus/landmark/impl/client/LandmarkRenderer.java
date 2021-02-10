package dev.hephaestus.landmark.impl.client;

import dev.hephaestus.landmark.api.LandmarkCache;
import dev.hephaestus.landmark.impl.LandmarkMod;
import dev.hephaestus.landmark.impl.item.DeedItem;
import dev.hephaestus.landmark.impl.util.Landmark;
import dev.hephaestus.landmark.api.LandmarkHolder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public class LandmarkRenderer extends RenderLayer {
    private LandmarkRenderer(String name, VertexFormat vertexFormat, int drawMode, int expectedBufferSize, boolean hasCrumbling, boolean translucent, Runnable startAction, Runnable endAction) {
        super(name, vertexFormat, drawMode, expectedBufferSize, hasCrumbling, translucent, startAction, endAction);
    }

    public static void init() {
        WorldRenderEvents.BEFORE_ENTITIES.register(context -> {
            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity player = client.player;

            if (player != null && player.getMainHandStack().getItem().isIn(LandmarkMod.SHOWS_BOUNDS)) {
                Vec3d camPos = context.camera().getPos();
                MatrixStack matrices = context.matrixStack();
                VertexConsumerProvider consumers = context.consumers();
                LandmarkHolder cache = LandmarkCache.getInstance(context.world());

                if (consumers == null) return;

                matrices.push();
                matrices.translate(-camPos.x, -camPos.y, -camPos.z);

                VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());

                ItemStack stack = player.getMainHandStack();
                CompoundTag tag = stack.getTag();

                if (stack.getItem() instanceof DeedItem && tag != null && !tag.isEmpty()) {
                    Landmark landmark = cache.getLandmark(tag.getUuid("landmark_id"));

                    if (landmark != null) {
                        Vector3f color = landmark.getColor();

                        if (tag.contains("marker")) {
                            BlockPos start = BlockPos.fromLong(tag.getLong("marker"));
                            BlockPos end;
                            HitResult result = MinecraftClient.getInstance().crosshairTarget;

                            if (result instanceof BlockHitResult) {
                                end = ((BlockHitResult) result).getBlockPos();
                            } else {
                                end = start;
                            }

                            BlockBox box = new BlockBox(start, end);
                            WorldRenderer.drawBox(matrices, lines, box.minX, box.minY, box.minZ, box.maxX + 1, box.maxY + 1, box.maxZ + 1, color.getX(), color.getY(), color.getZ(), 1);
                        }

                        for (BlockBox box : landmark.getBoxes()) {
                            WorldRenderer.drawBox(matrices, lines,
                                    box.minX,
                                    box.minY,
                                    box.minZ,
                                    box.maxX,
                                    box.maxY,
                                    box.maxZ,
                                    color.getX(), color.getY(), color.getZ(),
                                    1F);
                        }
                    }
                } else {
                    for (Landmark landmark : cache.getLandmarks()) {
                        Vector3f color = landmark.getColor();

                        for (BlockBox box : landmark.getBoxes()) {
                            WorldRenderer.drawBox(matrices, lines,
                                    box.minX,
                                    box.minY,
                                    box.minZ,
                                    box.maxX,
                                    box.maxY,
                                    box.maxZ,
                                    color.getX(), color.getY(), color.getZ(),
                                    0.5F);
                        }
                    }
                }

                context.matrixStack().pop();
            }
        });
    }
}
