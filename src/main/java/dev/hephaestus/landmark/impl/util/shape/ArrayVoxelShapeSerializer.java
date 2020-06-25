package dev.hephaestus.landmark.impl.util.shape;

import java.util.function.DoubleConsumer;

import dev.hephaestus.landmark.impl.util.Taggable;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.shape.ArrayVoxelShape;
import net.minecraft.util.shape.BitSetVoxelSet;

public class ArrayVoxelShapeSerializer implements Taggable<ArrayVoxelShape> {
	public static final ArrayVoxelShapeSerializer INSTANCE = new ArrayVoxelShapeSerializer();

	private static void putDoubleList(CompoundTag tag, String key, DoubleList list) {
		ListTag points = tag.getList(key, 6);
		list.forEach((DoubleConsumer) d -> points.add(DoubleTag.of(d)));
		tag.put(key, points);
	}

	private static DoubleList getDoubleList(CompoundTag tag, String key) {
		DoubleList list = new DoubleArrayList();
		ListTag listTag = tag.getList(key, 6);

		for (Tag t : listTag) {
			list.add(((DoubleTag) t).getDouble());
		}

		return list;
	}

	@Override
	public CompoundTag toTag(CompoundTag tag, ArrayVoxelShape voxelShape) {
		putDoubleList(tag, "xPoints", voxelShape.xPoints);
		putDoubleList(tag, "yPoints", voxelShape.yPoints);
		putDoubleList(tag, "zPoints", voxelShape.zPoints);

		tag.put("voxelSet", BitSetVoxelSetSerializer.INSTANCE.toTag(new CompoundTag(), (BitSetVoxelSet) voxelShape.voxels));

		return tag;
	}

	@Override
	public ArrayVoxelShape fromTag(CompoundTag tag) {
		return new ArrayVoxelShape(
				BitSetVoxelSetSerializer.INSTANCE.fromTag(tag.getCompound("voxelSet")),
				getDoubleList(tag, "xPoints"),
				getDoubleList(tag, "yPoints"),
				getDoubleList(tag, "zPoints")
		);
	}
}
