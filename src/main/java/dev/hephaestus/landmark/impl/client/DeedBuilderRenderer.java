package dev.hephaestus.landmark.impl.client;

import dev.hephaestus.landmark.impl.item.DeedItem;
import dev.hephaestus.landmark.impl.landmarks.CustomLandmarkTracker;
import dev.hephaestus.landmark.impl.landmarks.LandmarkNameTracker;
import dev.hephaestus.landmark.impl.util.DeedRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.network.PacketContext;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

import java.util.UUID;

public class DeedBuilderRenderer {
    private static CustomLandmarkTracker customLandmarkTracker;
    private static LandmarkNameTracker landmarkNameTracker;

    private static UUID ID;
    private static VoxelShape SHAPE;
    private static float RED = 1F;
    private static float GREEN = 1F;
    private static float BLUE = 1F;

    @Environment(EnvType.CLIENT)
    public static void render(BufferBuilderStorage bufferBuilders, MatrixStack matrices, ItemStack renderStack, Vec3d camPos) {
        if (renderStack.getItem() instanceof  DeedItem && renderStack.hasTag()) {
            CompoundTag tag = renderStack.getTag();
            if (tag != null && tag.contains("deed_id") && (ID == null || !ID.equals(tag.getUuid("deed_id")))) {
                clear();
                DeedRegistry.request(tag.getUuid("deed_id"));
            }

            if (tag != null && tag.contains("deed_name")) {
                Text text = Text.Serializer.fromJson(tag.getString("deed_name"));
                if (text != null && text.getStyle().getColor() != null) {
                    int color = text.getStyle().getColor().getRgb();
                    RED = ((float) ((color >>> 16) & 0xFF)) / 255F;
                    GREEN = ((float) ((color >>> 8) & 0xFF)) / 255F;
                    BLUE = ((float) (color & 0xFF)) / 255F;
                }
            }

            matrices.push();
            matrices.translate(-camPos.x, -camPos.y, -camPos.z);
            VertexConsumer vertexConsumer = bufferBuilders.getEffectVertexConsumers().getBuffer(RenderLayer.getLines());

            if (customLandmarkTracker != null && landmarkNameTracker != null) {
                for (BlockPos pos : customLandmarkTracker.getAll()) {
                    TextColor textColor = landmarkNameTracker.getLandmarkName(pos).getStyle().getColor();
                    int color = textColor == null ? 0xFFFFFFFF : textColor.getRgb();
                    float red = ((float) ((color >>> 16) & 0xFF)) / 255F;
                    float green = ((float) ((color >>> 8) & 0xFF)) / 255F;
                    float blue = ((float) (color & 0xFF)) / 255F;

                    customLandmarkTracker.get(pos).forEachBox((x1, y1, z1, x2, y2, z2) -> WorldRenderer.drawBox(matrices, vertexConsumer, x1, y1, z1, x2, y2, z2, red, green, blue, 0.5F));
                }
            }

            if (SHAPE != null && ID != null && tag != null && ID.equals(tag.getUuid("deed_id"))) {
                SHAPE.forEachBox((x1, y1, z1, x2, y2, z2) -> WorldRenderer.drawBox(matrices, vertexConsumer, x1, y1, z1, x2, y2, z2, RED, GREEN, BLUE, 1F));
            }

            matrices.pop();
        }
    }

    @Environment(EnvType.CLIENT)
    public static void apply(PacketContext context, PacketByteBuf buf) {
        ID = buf.readUuid();
        int boxCount = buf.readInt();

        VoxelShape shape = VoxelShapes.empty();
        for (int i = 0; i < boxCount; ++i) {
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble()
            ));
        }

        SHAPE = shape.simplify();
    }

    public static void clear() {
        ID = null;
        SHAPE = null;
        RED = 1F;
        GREEN = 1F;
        BLUE = 1F;
    }

    public static void captureBoxes(PacketContext context, PacketByteBuf buf) {
        CompoundTag compoundTag = buf.readCompoundTag();
        if (compoundTag != null) {
            customLandmarkTracker = new CustomLandmarkTracker(null);
            customLandmarkTracker.fromTag(compoundTag);
        }
    }

    public static void captureNames(PacketContext context, PacketByteBuf buf) {
        CompoundTag compoundTag = buf.readCompoundTag();
        if (compoundTag != null) {
            landmarkNameTracker = new LandmarkNameTracker(null);
            landmarkNameTracker.fromTag(compoundTag);
        }
    }
}
