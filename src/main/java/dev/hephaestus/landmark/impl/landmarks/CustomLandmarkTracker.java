package dev.hephaestus.landmark.impl.landmarks;

import dev.hephaestus.landmark.impl.LandmarkMod;
import dev.hephaestus.landmark.impl.util.SyncedState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CustomLandmarkTracker extends SyncedState {
//    public static Identifier CUSTOM_LANDMARKS_UPDATE = LandmarkMod.id("custom", "update");
//
//    private static final String ID = "custom_landmarks";
//
//    private final HashMap<BlockPos, VoxelShape> customLandmarks = new HashMap<>();
//
//    public CustomLandmarkTracker(MinecraftServer server) {
//        super(ID, CUSTOM_LANDMARKS_UPDATE, server);
//    }
//
//    public static void add(ServerWorld world, BlockPos pos, VoxelShape shape) {
//        CustomLandmarkTracker tracker = get(world);
//        tracker.customLandmarks.putIfAbsent(pos, shape);
//        tracker.markDirty();
//    }
//
//    public Collection<BlockPos> getAll() {
//        return this.customLandmarks.keySet();
//    }
//
//    public VoxelShape get(BlockPos pos) {
//        return this.customLandmarks.get(pos);
//    }
//
//    public static CustomLandmarkTracker get(ServerWorld world) {
//        return world.getPersistentStateManager().getOrCreate(() -> new CustomLandmarkTracker(world.getServer()), ID);
//    }
//
//    @Override
//    public void fromTag(CompoundTag tag) {
//        for (String key : tag.getKeys()) {
//            VoxelShape shape = VoxelShapes.empty();
//
//            ListTag listTag = tag.getList(key, 6);
//            for (int i = 0; i < listTag.size(); i += 6) {
//                shape = VoxelShapes.union(shape, VoxelShapes.cuboid(
//                        listTag.getDouble(i),
//                        listTag.getDouble(i + 1),
//                        listTag.getDouble(i + 2),
//                        listTag.getDouble(i + 3),
//                        listTag.getDouble(i + 4),
//                        listTag.getDouble(i + 5)
//                ));
//            }
//
//            this.customLandmarks.put(BlockPos.fromLong(Long.parseLong(key)), shape);
//        }
//
//        super.setTag(tag);
//    }
//
//    protected void setTag(CompoundTag tag) {
//        for (Map.Entry<BlockPos, VoxelShape> entry : this.customLandmarks.entrySet()) {
//            Collection<Box> boxes = entry.getValue().getBoundingBoxes();
//
//            ListTag boxList = new ListTag();
//            for (Box box : boxes) {
//                boxList.add(DoubleTag.of(box.minX));
//                boxList.add(DoubleTag.of(box.minY));
//                boxList.add(DoubleTag.of(box.minZ));
//                boxList.add(DoubleTag.of(box.maxX));
//                boxList.add(DoubleTag.of(box.maxY));
//                boxList.add(DoubleTag.of(box.maxZ));
//            }
//
//            tag.put(String.valueOf(entry.getKey().asLong()), boxList);
//        }
//
//        super.setTag(tag);
//    }
}
