package dev.hephaestus.landmark.impl.util.shape.lithium;

import dev.hephaestus.landmark.impl.util.Taggable;
import dev.hephaestus.landmark.impl.util.shape.BitSetVoxelSetSerializer;
import me.jellysquid.mods.lithium.common.shapes.VoxelShapeEmpty;
import me.jellysquid.mods.lithium.common.shapes.VoxelShapeSimpleCube;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.BitSetVoxelSet;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

public class LithiumVoxelShapeSerializer implements Taggable<VoxelShape> {
	public static final LithiumVoxelShapeSerializer INSTANCE = new LithiumVoxelShapeSerializer();

	@Override
	public CompoundTag toTag(CompoundTag tag, VoxelShape voxelShape) {
		if (voxelShape instanceof VoxelShapeSimpleCube) {
			tag.putString("type", "lithium_simple");
			tag.put("voxelSet", BitSetVoxelSetSerializer.INSTANCE.toTag(new CompoundTag(), (BitSetVoxelSet) voxelShape.voxels));
			tag.putDouble("x1", voxelShape.getMin(Direction.Axis.X));
			tag.putDouble("y1", voxelShape.getMin(Direction.Axis.Y));
			tag.putDouble("z1", voxelShape.getMin(Direction.Axis.Z));
			tag.putDouble("x2", voxelShape.getMax(Direction.Axis.X));
			tag.putDouble("y2", voxelShape.getMax(Direction.Axis.Y));
			tag.putDouble("z2", voxelShape.getMax(Direction.Axis.Z));
			return tag;
		} else if (voxelShape instanceof VoxelShapeEmpty) {
			tag.putString("type", "lithium_empty");
			return tag;
		} else {
			return new CompoundTag();
		}
	}

	@Override
	public VoxelShape fromTag(CompoundTag tag) {
		if (tag.getString("type").equals("lithium_simple")) {
			return new VoxelShapeSimpleCube(
					BitSetVoxelSetSerializer.INSTANCE.fromTag(tag.getCompound("voxelSet")),
					tag.getDouble("x1"),
					tag.getDouble("y1"),
					tag.getDouble("z1"),
					tag.getDouble("x2"),
					tag.getDouble("y2"),
					tag.getDouble("z2")
			);
		} else {
			return VoxelShapes.empty();
		}
	}
}
