package dev.hephaestus.landmark.impl.util.shape;

import dev.hephaestus.landmark.impl.util.Taggable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.shape.BitSetVoxelSet;
import net.minecraft.util.shape.SimpleVoxelShape;

public class SimpleVoxelShapeSerializer implements Taggable<SimpleVoxelShape> {
	public static final SimpleVoxelShapeSerializer INSTANCE = new SimpleVoxelShapeSerializer();

	@Override
	public CompoundTag toTag(CompoundTag tag, SimpleVoxelShape voxelShape) {
		tag.put("voxelSet", BitSetVoxelSetSerializer.INSTANCE.toTag(new CompoundTag(), (BitSetVoxelSet) voxelShape.voxels));
		return tag;
	}

	@Override
	public SimpleVoxelShape fromTag(CompoundTag tag) {
		return new SimpleVoxelShape(BitSetVoxelSetSerializer.INSTANCE.fromTag(tag.getCompound("voxelSet")));
	}
}
