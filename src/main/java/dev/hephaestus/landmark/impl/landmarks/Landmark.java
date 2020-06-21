package dev.hephaestus.landmark.impl.landmarks;

import com.google.common.collect.ConcurrentHashMultiset;
import io.netty.util.internal.ConcurrentSet;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;

public abstract class Landmark {
    protected final Collection<ChunkPos> chunks = new ConcurrentSet<>();

    private World world;
    private UUID id;
    private Text name;
    private Vector3f color;

    protected Landmark(World world, UUID id, Text name) {
        this.world = world;
        this.id = id;
        this.setName(name);
    }

    public final void setName(Text name) {
        this.name = name;
        TextColor color = this.name.getStyle().getColor();

        if (color != null) {
            int i = color.rgb;
            this.color = new Vector3f(
                    ((float) ((i >>> 16) & 0xFF)) / 255F,
                    ((float) ((i >>> 8) & 0xFF)) / 255F,
                    ((float) (i & 0xFF)) / 255F
            );
        } else {
            this.color = new Vector3f(1F, 1F, 1F);
        }
    }

    public abstract boolean add(LandmarkSection section);

    public final World getWorld() {
        return this.world;
    }

    public final UUID getId() {
        return this.id;
    }

    public final Text getName() {
        return this.name;
    }

    public final Vector3f getColor() {
        return this.color;
    }

    public CompoundTag toTag(CompoundTag tag) {
        tag.putUuid("id", this.id);
        tag.putString("name", Text.Serializer.toJson(this.name));

        ListTag chunks = tag.getList("chunks", 4);
        for (ChunkPos pos : this.chunks) {
            chunks.add(LongTag.of(pos.toLong()));
        }

        tag.put("chunks", chunks);

        return tag;
    }

    public Landmark fromTag(World world, CompoundTag tag) {
        this.world = world;
        this.id = tag.getUuid("id");
        this.setName(Text.Serializer.fromJson(tag.getString("name")));

        ListTag chunks = tag.getList("chunks", 4);
        for (Tag chunk : chunks) {
            this.chunks.add(new ChunkPos(((LongTag) chunk).getLong()));
        }

        return this;
    }

    public static Landmark from(World world, CompoundTag tag) {
        String type = tag.getString("type");
        switch (type) {
            case "player":
                return new PlayerLandmark(world).fromTag(world, tag);
            default:
                return new GeneratedLandmark(world, LiteralText.EMPTY);
        }
    }

    public final Iterable<? extends ChunkPos> getChunks() {
        return this.chunks;
    }
}
