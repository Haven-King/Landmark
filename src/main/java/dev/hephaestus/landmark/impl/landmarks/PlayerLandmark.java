package dev.hephaestus.landmark.impl.landmarks;

import dev.hephaestus.landmark.api.LandmarkType;
import dev.hephaestus.landmark.impl.LandmarkMod;
import dev.hephaestus.landmark.impl.names.NameGenerator;
import dev.hephaestus.landmark.impl.world.LandmarkTrackingComponent;
import dev.hephaestus.landmark.impl.world.chunk.LandmarkChunkComponent;
import net.minecraft.nbt.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;

import java.util.*;

public class PlayerLandmark extends Landmark {
    private VoxelShape shape;
    private double volume;
    private boolean built = false;

    public PlayerLandmark(World world) {
        this(world, LiteralText.EMPTY);
    }

    public PlayerLandmark(World world, Text name) {
        super(world, UUID.randomUUID(), name);
    }

    @Override
    public CompoundTag toTag(CompoundTag tag) {
        tag.putString("type", "player");


        if (this.shape != null) {
            Collection<Box> boxes = this.shape.getBoundingBoxes();

            tag.putInt("box_count", boxes.size());

            ListTag boxesTag = tag.getList("boxes", 6);
            for (Box box : boxes) {
                boxesTag.add(DoubleTag.of(box.minX));
                boxesTag.add(DoubleTag.of(box.minY));
                boxesTag.add(DoubleTag.of(box.minZ));
                boxesTag.add(DoubleTag.of(box.maxX));
                boxesTag.add(DoubleTag.of(box.maxY));
                boxesTag.add(DoubleTag.of(box.maxZ));
            }

            tag.put("boxes", boxesTag);
        }

        return super.toTag(tag);
    }

    @Override
    public Landmark fromTag(World world, CompoundTag tag) {
        super.fromTag(world, tag);

        this.volume = tag.getDouble("volume");

        if (tag.contains("boxes") && tag.contains("box_count")) {
            ListTag boxes = tag.getList("boxes", 6);

            for (int i = 0; i < tag.getInt("box_count"); i += 6) {
                VoxelShape newShape = VoxelShapes.cuboid(
                        boxes.getDouble(i),
                        boxes.getDouble(i + 1),
                        boxes.getDouble(i + 2),
                        boxes.getDouble(i + 3),
                        boxes.getDouble(i + 4),
                        boxes.getDouble(i + 5)
                );
                this.shape = this.shape == null ? newShape : VoxelShapes.union(this.shape, newShape);
            }
        }

        return this;
    }

    public boolean add(LandmarkSection section, double maxVolume) {
        VoxelShape added = VoxelShapes.cuboid(
                section.minX,
                section.minY,
                section.minZ,
                section.maxX,
                section.maxY,
                section.maxZ
        );

        VoxelShape newShape = this.shape == null ? added : VoxelShapes.union(this.shape, added);

        List<Double> volumes = new LinkedList<>();
        newShape.forEachBox((x1, y1, z1, x2, y2, z2) -> volumes.add((x2 - x1) * (y2 - y1) * (z2 - z1)));

        double volume = 0D;
        for (double d : volumes) {
            volume += d;
        }

        if (volume <= maxVolume) {
            this.shape = newShape.simplify();
            this.volume = volume;
            this.built = false;
            return true;
        }

        return false;
    }


    public boolean add(LandmarkSection section) {
        return this.add(section, Double.MAX_VALUE);
    }

    public void makeSections(ServerWorld world) {
        if (this.shape != null) {
            for (ChunkPos pos : chunks) {
                LandmarkChunkComponent component = LandmarkMod.CHUNK_COMPONENT.get(world.getChunk(pos.x, pos.z));
                component.remove(this);
            }

            this.shape.forEachBox(((minX, minY, minZ, maxX, maxY, maxZ) -> {
                LandmarkSection section = new LandmarkSection(this.getId(), minX, minY, minZ, maxX, maxY, maxZ);
                Collection<ChunkPos> chunks = section.getChunks();
                this.chunks.addAll(chunks);

                for (ChunkPos pos : chunks) {
                    LandmarkChunkComponent component = LandmarkMod.CHUNK_COMPONENT.get(world.getChunk(pos.x, pos.z));
                    component.add(section);
                }
            }));

            LandmarkTrackingComponent tracker = LandmarkTrackingComponent.of(world);

            for (ChunkPos pos : chunks) {
                tracker.put(pos, this);
                LandmarkChunkComponent component = LandmarkMod.CHUNK_COMPONENT.get(world.getChunk(pos.x, pos.z));
                component.sync();
            }

            tracker.sync();
        }

        this.built = true;
    }

    public void buildIfReady(ServerWorld world) {
        if (!this.isBuilt()) {
            boolean allChunksLoaded = true;
            for (ChunkPos pos : this.chunks) {
                allChunksLoaded &= world.isChunkLoaded(pos.x, pos.z);
            }

            if (allChunksLoaded) {
                this.makeSections(world);
            }
        }
    }

    public boolean isBuilt() {
        return this.built;
    }

    public double volume() {
        return this.volume;
    }

    public void addChunks(Collection<ChunkPos> chunks) {
        this.chunks.addAll(chunks);
    }
}
