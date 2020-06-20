package dev.hephaestus.landmark.impl.landmarks;

import dev.hephaestus.landmark.api.LandmarkType;
import dev.hephaestus.landmark.impl.LandmarkMod;
import dev.hephaestus.landmark.impl.names.NameGenerator;
import dev.hephaestus.landmark.impl.world.chunk.LandmarkChunkComponent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

import java.util.*;

public class Landmark {
    public final UUID uuid;

    private Text name;
    private float red;
    private float green;
    private float blue;
    private final Collection<ChunkPos> chunks;
    private VoxelShape shape;
    private double volume;
    private boolean built = false;

    private Landmark(UUID uuid, Text name, double volume) {
        this.uuid = uuid;
        this.setName(null, name);
        this.volume = volume;
        this.shape = VoxelShapes.empty();
        this.chunks = new HashSet<>();
    }

    public Landmark(Text name) {
        this(UUID.randomUUID(), name, 0D);
    }

    public Landmark() {
        this(new LiteralText(""));
    }

    public Landmark(LandmarkType type) {
        this(NameGenerator.generate(type.getNameGeneratorId()));
    }

    public CompoundTag toTag(CompoundTag tag) {
        tag.putUuid("uuid", this.uuid);
        tag.putString("name", Text.Serializer.toJson(this.name));
        tag.putDouble("volume", this.volume);

        ListTag chunks = tag.getList("chunks", 4);

        for (ChunkPos pos : this.chunks) {
            chunks.add(LongTag.of(pos.toLong()));
        }

        tag.put("chunks", chunks);

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

        return tag;
    }

    public static Landmark fromTag(CompoundTag tag) {
        UUID uuid = tag.getUuid("uuid");
        Text name = Text.Serializer.fromJson(tag.getString("name"));
        double volume = tag.getDouble("volume");
        Landmark landmark = new Landmark(uuid, name, volume);

        ListTag chunks = tag.getList("chunks", 4);
        for (Tag chunk : chunks) {
            landmark.chunks.add(new ChunkPos(((LongTag)chunk).getLong()));
        }

        ListTag boxes = tag.getList("boxes", 6);
        for (int i = 0; i < tag.getInt("box_count"); i += 6) {
            landmark.shape = VoxelShapes.union(landmark.shape, VoxelShapes.cuboid(
                    boxes.getDouble(i),
                    boxes.getDouble(i + 1),
                    boxes.getDouble(i + 2),
                    boxes.getDouble(i + 3),
                    boxes.getDouble(i + 4),
                    boxes.getDouble(i + 5)
            ));
        }

        return landmark;
    }

    public void setName(ServerWorld world, Text name) {
        this.name = name;
        TextColor color = this.name.getStyle().getColor();

        if (color != null) {
            int i = color.rgb;
            this.red = ((float) ((i >>> 16) & 0xFF)) / 255F;
            this.green = ((float) ((i >>> 8) & 0xFF)) / 255F;
            this.blue = ((float) (i & 0xFF)) / 255F;
        } else {
            this.red = 1F;
            this.green = 1F;
            this.blue = 1F;
        }
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
            this.shape = newShape/*.simplify()*/;
            this.volume = volume;
            this.built = false;
            return true;
        }

        return false;
    }

    public void add(VoxelShape... boxes) {
        VoxelShapes.union()
    }

    public void add(Landmark.Section section) {
        this.add(VoxelShapes.cuboid(section.minX, section.minY, section.minZ, section.maxX, section.maxY, section.maxZ), Double.MAX_VALUE);
    }

    public void makeSections(ServerWorld world) {
        for (ChunkPos pos : chunks) {
            LandmarkChunkComponent component = LandmarkMod.LANDMARKS_COMPONENT.get(world.getChunk(pos.x, pos.z));
            component.remove(this);
        }

        this.shape.forEachBox(((minX, minY, minZ, maxX, maxY, maxZ) -> {
            Section section = new Section(this.uuid, minX, minY, minZ, maxX, maxY, maxZ, this.red, this.green, this.blue);
            Collection<ChunkPos> chunks = section.getChunks();
            this.chunks.addAll(chunks);

            for (ChunkPos pos : chunks) {
                LandmarkChunkComponent component = LandmarkMod.LANDMARKS_COMPONENT.get(world.getChunk(pos.x, pos.z));
                component.add(section);
            }
        }));

        LandmarkTracker tracker = LandmarkTracker.get(world);

        for (ChunkPos pos : chunks) {
            tracker.put(pos, this);
            LandmarkChunkComponent component = LandmarkMod.LANDMARKS_COMPONENT.get(world.getChunk(pos.x, pos.z));
            component.sync();
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

    public Text getName() {
        return this.name;
    }

    public void addChunks(Collection<ChunkPos> chunks) {
        this.chunks.addAll(chunks);
    }

    public Collection<ChunkPos> getChunks() {
        return this.chunks;
    }

    public static class Section implements Comparable<Section> {
        public final UUID parent;
        public final double minX;
        public final double minY;
        public final double minZ;
        public final double maxX;
        public final double maxY;
        public final double maxZ;
        public final double volume;
        public final float red;
        public final float green;
        public final float blue;
        private final Collection<ChunkPos> chunks;

        public Section(UUID parent, BlockBox boundingBox, float red, float green, float blue) {
            this(
                    parent,
                    boundingBox.minX,
                    boundingBox.minY,
                    boundingBox.minZ,
                    boundingBox.maxX,
                    boundingBox.maxY,
                    boundingBox.maxZ,
                    red,
                    blue,
                    green
            );
        }

        public Section(UUID parent, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, float red, float green, float blue) {
            this.parent = parent;
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
            this.red = red;
            this.blue = blue;
            this.green = green;
            this.volume = (this.maxX - this.minX) * (this.maxY - this.minY) * (this.maxZ - this.minZ);
            chunks = new HashSet<>();
            for (double x = minX; x <= maxX; x += 16) {
                for (double z = minZ; z <= maxZ; z += 16) {
                    this.chunks.add(new ChunkPos((int)x >> 4, (int)z >> 4));
                }
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Section section = (Section) o;
            return Double.compare(section.minX, minX) == 0 &&
                    Double.compare(section.minY, minY) == 0 &&
                    Double.compare(section.minZ, minZ) == 0 &&
                    Double.compare(section.maxX, maxX) == 0 &&
                    Double.compare(section.maxY, maxY) == 0 &&
                    Double.compare(section.maxZ, maxZ) == 0 &&
                    parent.equals(section.parent);
        }

        @Override
        public int hashCode() {
            return Objects.hash(parent, minX, minY, minZ, maxX, maxY, maxZ);
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
        public int compareTo(Section section) {
            // This is backwards because we want them inserted into the queue in reverse order.
            return Double.compare(section.volume, this.volume);
        }

        public CompoundTag toTag(CompoundTag tag) {
            tag.putUuid("parent", this.parent);
            tag.putDouble("minX", this.minX);
            tag.putDouble("minY", this.minY);
            tag.putDouble("minZ", this.minZ);
            tag.putDouble("maxX", this.maxX);
            tag.putDouble("maxY", this.maxY);
            tag.putDouble("maxZ", this.maxZ);
            tag.putFloat("red", this.red);
            tag.putFloat("green", this.green);
            tag.putFloat("blue", this.blue);

            ListTag chunksTag = tag.getList("chunks", 4);
            for (ChunkPos pos : this.chunks) {
                chunksTag.add(LongTag.of(pos.toLong()));
            }

            return tag;
        }

        public static Section fromTag(CompoundTag tag) {
            return new Section(
                    tag.getUuid("parent"),
                    tag.getDouble("minX"),
                    tag.getDouble("minY"),
                    tag.getDouble("minZ"),
                    tag.getDouble("maxX"),
                    tag.getDouble("maxY"),
                    tag.getDouble("maxZ"),
                    tag.getFloat("red"),
                    tag.getFloat("green"),
                    tag.getFloat("blue")
            );
        }

        @Environment(EnvType.CLIENT)
        public void render(MatrixStack matrices, VertexConsumer vertexConsumer) {
            float alpha = 0.5F;

            PlayerEntity player = MinecraftClient.getInstance().player;
            if (player != null) {
                CompoundTag tag = player.getMainHandStack().getOrCreateTag();
                if (tag.contains("deed_id")) {
                    UUID id = tag.getUuid("deed_id");
                    alpha = id.equals(this.parent) ? 1F : 0.25F;
                }
            }

            WorldRenderer.drawBox(matrices, vertexConsumer, this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ, this.red, this.green, this.blue, alpha);
        }
    }
}
