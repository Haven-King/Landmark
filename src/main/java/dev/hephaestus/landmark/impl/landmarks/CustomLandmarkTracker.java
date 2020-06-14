package dev.hephaestus.landmark.impl.landmarks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.PersistentState;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomLandmarkTracker extends PersistentState {
    private static final String ID = "custom_landmarks";

    private final HashMap<BlockPos, VoxelShape> customLandmarks = new HashMap<>();

    public CustomLandmarkTracker() {
        super(ID);
    }

    public static void add(ServerWorld world, BlockPos pos, VoxelShape shape) {
        CustomLandmarkTracker tracker = get(world);
        tracker.customLandmarks.putIfAbsent(pos, shape);
        tracker.markDirty();
    }

    public Collection<BlockPos> getAll() {
        return this.customLandmarks.keySet();
    }

    public VoxelShape get(BlockPos pos) {
        return this.customLandmarks.get(pos);
    }

    public static CustomLandmarkTracker get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(CustomLandmarkTracker::new, ID);
    }

    @Override
    public void fromTag(CompoundTag tag) {
        CompoundTag landmarkTag = tag.getCompound("custom_landmarks");

        for (String key : landmarkTag.getKeys()) {
            VoxelShape shape = VoxelShapes.empty();

            ListTag listTag = landmarkTag.getList(key, 6);
            for (int i = 0; i < listTag.size(); i += 6) {
                shape = VoxelShapes.union(shape, VoxelShapes.cuboid(
                        listTag.getDouble(i),
                        listTag.getDouble(i + 1),
                        listTag.getDouble(i + 2),
                        listTag.getDouble(i + 3),
                        listTag.getDouble(i + 4),
                        listTag.getDouble(i + 5)
                ));
            }

            this.customLandmarks.put(BlockPos.fromLong(Long.parseLong(key)), shape);
        }
    }

    @Override
    public CompoundTag toTag(CompoundTag tag) {
        CompoundTag landmarkTag = new CompoundTag();

        for (Map.Entry<BlockPos, VoxelShape> entry : this.customLandmarks.entrySet()) {
            Collection<Box> boxes = entry.getValue().getBoundingBoxes();

            ListTag boxList = new ListTag();
            for (Box box : boxes) {
                boxList.add(DoubleTag.of(box.minX));
                boxList.add(DoubleTag.of(box.maxX));
                boxList.add(DoubleTag.of(box.minY));
                boxList.add(DoubleTag.of(box.maxY));
                boxList.add(DoubleTag.of(box.minZ));
                boxList.add(DoubleTag.of(box.maxZ));
            }

            landmarkTag.put(String.valueOf(entry.getKey().asLong()), boxList);
        }

        tag.put("custom_landmarks", landmarkTag);

        return tag;
    }
}
