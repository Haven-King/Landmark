package dev.hephaestus.landmark.impl.landmarks;

import dev.hephaestus.landmark.api.LandmarkType;
import dev.hephaestus.landmark.impl.names.NameGenerator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

import java.util.*;

public class Landmark implements Nameable {
    private final UUID uuid;
    private final Text name;
    private final Collection<ChunkPos> chunks;
    private TextColor textColor;
    private VoxelShape shape;
    private Collection<Section> sections;
    private double volume;

    public Landmark(Text name) {
        this.name = name;
        this.textColor = this.name.getStyle().getColor();
        this.shape = VoxelShapes.empty();
        this.volume = 0D;
        uuid = UUID.randomUUID();
        chunks = new HashSet<>();
    }

    public Landmark(LandmarkType type) {
        this(NameGenerator.generate(type.getNameGeneratorId()));
    }

    public boolean add(VoxelShape shape, double maxVolume) {
        VoxelShape newShape = VoxelShapes.union(this.shape, shape).simplify();

        List<Double> volumes = new LinkedList<>();
        newShape.forEachBox((x1, y1, z1, x2, y2, z2) -> volumes.add((x2 - x1) * (y2 - y1) * (z2 - z1)));

        double volume = 0D;
        for (double d : volumes) {
            volume += d;
        }

        if (volume <= maxVolume) {
            this.shape = newShape;
            this.volume = volume;
            return true;
        }

        return false;
    }

    public BlockPos getCenter() {
        return new BlockPos(this.shape.getBoundingBox().getCenter());
    }

    public void makeSections(Collection<Section> sections) {
        this.shape.forEachBox(((minX, minY, minZ, maxX, maxY, maxZ) -> {
            Section section = new Section(this.uuid, this.name, minX, minY, minZ, maxX, maxY, maxZ);
            this.chunks.addAll(section.getChunks());
            sections.add(section);
        }));
    }

    public double volume() {
        return this.volume;
    }

    @Override
    public Text getName() {
        return this.name;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public int getColor() {
        return this.textColor.getRgb();
    }

    public static class Section implements Nameable, Comparable<Section> {
        private final UUID parent;
        private final Text name;
        private final TextColor textColor;
        public final double minX;
        public final double minY;
        public final double minZ;
        public final double maxX;
        public final double maxY;
        public final double maxZ;
        public final double volume;
        private final Collection<ChunkPos> chunks;


        public Section(UUID parent, Text name, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
            this.parent = parent;
            this.name = name;
            this.textColor = name.getStyle().getColor();
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
            this.volume = (this.maxX - this.minX) * (this.maxY - this.minY) * (this.maxZ - this.minZ);
            chunks = new HashSet<>();
            for (double x = minX; x <= maxX; x += 16) {
                for (double z = minZ; z <= maxZ; z += 16) {
                    this.chunks.add(new ChunkPos((int) x, (int) z));
                }
            }
        }

        public boolean contains(double x, double y, double z) {
            return x >= this.minX && x < this.maxX && y >= this.minY && y < this.maxY && z >= this.minZ && z < this.maxZ;
        }

        public boolean matches(UUID parent) {
            return parent.equals(this.parent);
        }

        public Collection<ChunkPos> getChunks() {
            return chunks;
        }

        @Override
        public Text getName() {
            return this.name;
        }

        @Override
        @Environment(EnvType.CLIENT)
        public int getColor() {
            return this.textColor.getRgb();
        }

        @Override
        public int compareTo(Section section) {
            // This is backwards because we want them inserted into the queue in reverse order.
            return Double.compare(section.volume, this.volume);
        }
    }
}
