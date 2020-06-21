package dev.hephaestus.landmark.impl.landmarks;

import dev.hephaestus.landmark.api.LandmarkType;
import dev.hephaestus.landmark.impl.LandmarkMod;
import dev.hephaestus.landmark.impl.names.NameGenerator;
import dev.hephaestus.landmark.impl.world.chunk.LandmarkChunkComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import java.util.*;

public class GeneratedLandmark extends Landmark {
    private Collection<LandmarkSection> sections = new PriorityQueue<>();
    private Collection<ChunkPos> pendingChunks = new HashSet<>();

    public GeneratedLandmark(World world, Text name) {
        super(world, UUID.randomUUID(), name);
    }

    public GeneratedLandmark(World world, LandmarkType type) {
        this(world, NameGenerator.generate(type.getNameGeneratorId()));
    }

    @Override
    public boolean add(LandmarkSection section) {
        this.chunks.addAll(section.getChunks());
        this.pendingChunks.addAll(section.getChunks());
        this.sections.add(section);
        return true;
    }

    @Override
    public CompoundTag toTag(CompoundTag tag) {
        tag.putString("type", "generated");
        return super.toTag(tag);
    }

    public boolean needsResolving() {
        return !this.pendingChunks.isEmpty();
    }

    public static void resolve(GeneratedLandmark landmark) {
        for (ChunkPos pos : landmark.chunks) {
            if (landmark.getWorld().isChunkLoaded(pos.x, pos.z)) {
                LandmarkChunkComponent component = LandmarkChunkComponent.of(landmark.getWorld().getChunk(pos.x, pos.z));

                for (LandmarkSection section : landmark.sections) {
                    if (section.getChunks().contains(pos)) {
                        component.add(section);
                    }
                }

                component.sync();
            }
        }

        landmark.chunks.removeIf((pos) -> landmark.getWorld().isChunkLoaded(pos.x, pos.z));
    }
}
