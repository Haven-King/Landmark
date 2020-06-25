package dev.hephaestus.landmark.impl.util.shape;

import dev.hephaestus.landmark.impl.util.Taggable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.BitSetVoxelSet;

public class BitSetVoxelSetSerializer implements Taggable<BitSetVoxelSet> {
	public static final BitSetVoxelSetSerializer INSTANCE = new BitSetVoxelSetSerializer();

	@Override
	public CompoundTag toTag(CompoundTag tag, BitSetVoxelSet voxelSet) {
		tag.putInt("xSize", voxelSet.getXSize());
		tag.putInt("ySize", voxelSet.getYSize());
		tag.putInt("zSize", voxelSet.getZSize());
		tag.putInt("minX", voxelSet.getMin(Direction.Axis.X));
		tag.putInt("minY", voxelSet.getMin(Direction.Axis.Y));
		tag.putInt("minZ", voxelSet.getMin(Direction.Axis.Z));
		tag.putInt("maxX", voxelSet.getMax(Direction.Axis.X));
		tag.putInt("maxY", voxelSet.getMax(Direction.Axis.Y));
		tag.putInt("maxZ", voxelSet.getMax(Direction.Axis.Z));
		tag.put("bitset", BitSetSerializer.INSTANCE.toTag(new CompoundTag(), voxelSet.storage));

		return tag;
	}

	@Override
	public BitSetVoxelSet fromTag(CompoundTag tag) {
		BitSetVoxelSet voxelSet = new BitSetVoxelSet(
				tag.getInt("xSize"),
				tag.getInt("ySize"),
				tag.getInt("zSize"),
				tag.getInt("minX"),
				tag.getInt("minY"),
				tag.getInt("minZ"),
				tag.getInt("maxX"),
				tag.getInt("maxY"),
				tag.getInt("maxZ")
		);

		voxelSet.storage = BitSetSerializer.INSTANCE.fromTag(tag.getCompound("bitset"));

		return voxelSet;
	}
}
