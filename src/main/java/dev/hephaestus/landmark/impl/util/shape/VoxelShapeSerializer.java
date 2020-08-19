package dev.hephaestus.landmark.impl.util.shape;

import dev.hephaestus.landmark.impl.util.Taggable;
//import dev.hephaestus.landmark.impl.util.shape.lithium.LithiumVoxelShapeSerializer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.shape.ArrayVoxelShape;
import net.minecraft.util.shape.SimpleVoxelShape;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

import net.fabricmc.loader.api.FabricLoader;

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
//		} else if (FabricLoader.getInstance().isModLoaded("lithium")) {
//			return LithiumVoxelShapeSerializer.INSTANCE.toTag(tag, voxelShape);
		} else {
			return new CompoundTag();
		}
	}

	@Override
	public VoxelShape fromTag(CompoundTag tag) {
		switch (tag.getString("type")) {
		case "array": return ArrayVoxelShapeSerializer.INSTANCE.fromTag(tag);
		case "simple": return SimpleVoxelShapeSerializer.INSTANCE.fromTag(tag);
		default:
//			if (FabricLoader.getInstance().isModLoaded("lithium")) {
//				return LithiumVoxelShapeSerializer.INSTANCE.fromTag(tag);
//			} else {
				return VoxelShapes.empty();
//			}
		}
	}
}
