package dev.hephaestus.landmark.impl.util.shape;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.shape.ArrayVoxelShape;
import net.minecraft.util.shape.BitSetVoxelSet;
import net.minecraft.util.shape.SimpleVoxelShape;
import net.minecraft.util.shape.VoxelShape;

public class VoxelShapeSerializer implements Serializer<VoxelShape> {
    public static final VoxelShapeSerializer INSTANCE = new VoxelShapeSerializer();

    @Override
    public CompoundTag toTag(VoxelShape object, CompoundTag tag) {
        if (object instanceof ArrayVoxelShape) {
            tag.putString("type", "array");
        } else if (object instanceof SimpleVoxelShape) {

        }

        return null;
    }

    @Override
    public VoxelShape fromTag(CompoundTag tag) {
        try {
            String type = tag.getString("type");

            switch (type) {
                case "array":
                    DoubleList xPoints = fromTag(tag.getList("xPoints", 6));
                    DoubleList yPoints = fromTag(tag.getList("yPoints", 6));
                    DoubleList zPoints = fromTag(tag.getList("zPoints", 6));

                    return new ArrayVoxelShape(new BitSetVoxelSet(xPoints.size(), yPoints.size(), zPoints.size()),
                            xPoints,
                            yPoints,
                            zPoints);
                case "simple":
                    throw new IllegalStateException("Error creating VoxelShape from tag: unsupported type: 'SimpleVoxelShape'");

                case "sliced":
                    throw new IllegalStateException("Error creating VoxelShape from tag: unsupported type: 'SlicedVoxelShape'");

                default:
                    throw new IllegalStateException("Error creating VoxelShape from tag: invalid type");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error creating VoxelShape from tag");
        }
    }

    private static DoubleList fromTag(ListTag tag) {
        DoubleList points = new DoubleArrayList();

            for (Tag d : tag) {
                points.add(((DoubleTag) d).getDouble());
            }

        return points;
    }
}
