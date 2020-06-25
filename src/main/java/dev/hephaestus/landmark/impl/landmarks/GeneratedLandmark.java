package dev.hephaestus.landmark.impl.landmarks;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.UUID;

import dev.hephaestus.landmark.impl.world.chunk.LandmarkChunkComponent;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.MutableText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

public class GeneratedLandmark extends Landmark {
	private final Collection<LandmarkSection> sections = new PriorityQueue<>();
	private final Collection<ChunkPos> pendingChunks = new HashSet<>();

	private final BlockPos center;

	public GeneratedLandmark(World world, BlockPos pos, MutableText name) {
		super(world, UUID.randomUUID(), name);
		this.center = pos;
	}

	public BlockPos getCenter() {
		return this.center;
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
		tag.putLong("center", this.center.asLong());
		return super.toTag(tag);
	}

	public boolean needsResolving() {
		return !this.pendingChunks.isEmpty();
	}

	public static void resolve(GeneratedLandmark landmark) {
		if (landmark.needsResolving()) {
			for (ChunkPos pos : landmark.pendingChunks) {
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

			landmark.pendingChunks.removeIf((pos) -> landmark.getWorld().isChunkLoaded(pos.x, pos.z));
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		GeneratedLandmark that = (GeneratedLandmark) o;
		return Objects.equals(center, that.center);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), center);
	}
}
