package dev.hephaestus.landmark.impl.util.shape;

import dev.hephaestus.landmark.impl.util.Taggable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.shape.ArrayVoxelShape;
import net.minecraft.util.shape.SimpleVoxelShape;
import net.minecraft.util.shape.VoxelShape;

public class VoxelShapeSerializer implements Taggable<VoxelShape> {
	// Fuck slices
	public static final VoxelShapeSerializer INSTANCE = new VoxelShapeSerializer();

	@Override
	public CompoundTag toTag(CompoundTag tag, VoxelShape voxelShape) {
		if (voxelShape instanceof ArrayVoxelShape) {
			ArrayVoxelShapeSerializer.INSTANCE.toTag(tag, (ArrayVoxelShape) voxelShape);
			tag.putString("type", "array");
			return tag;
		} else if (voxelShape instanceof SimpleVoxelShape) {
			SimpleVoxelShapeSerializer.INSTANCE.toTag(tag, (SimpleVoxelShape) voxelShape);
			tag.putString("type", "simple");
			return tag;
		} else {
			return null;
		}
	}

	@Override
	public VoxelShape fromTag(CompoundTag tag) {
		switch (tag.getString("type")) {
			case "array": return ArrayVoxelShapeSerializer.INSTANCE.fromTag(tag);
			case "simple": return SimpleVoxelShapeSerializer.INSTANCE.fromTag(tag);
			default: return null;
		}
	}
}
