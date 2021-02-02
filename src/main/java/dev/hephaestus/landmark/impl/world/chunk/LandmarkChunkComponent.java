package dev.hephaestus.landmark.impl.world.chunk;

import java.util.Collection;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.UUID;
import java.util.stream.Collectors;

import dev.hephaestus.landmark.impl.LandmarkMod;
import dev.hephaestus.landmark.impl.landmarks.Landmark;
import dev.hephaestus.landmark.impl.landmarks.LandmarkSection;
import dev.hephaestus.landmark.impl.landmarks.PlayerLandmark;
import dev.onyxstudios.cca.api.v3.component.ComponentV3;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import nerdhub.cardinal.components.api.component.Component;
import nerdhub.cardinal.components.api.util.sync.ChunkSyncedComponent;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.Chunk;

public class LandmarkChunkComponent implements AutoSyncedComponent {
	private Queue<LandmarkSection> landmarkSections = new PriorityQueue<>();
	private final Chunk chunk;

	public LandmarkChunkComponent(Chunk chunk) {
		this.chunk = chunk;
	}

	@Override
	public void readFromNbt(CompoundTag tag) {
		this.landmarkSections = new PriorityQueue<>();
		ListTag landmarkTag = tag.getList("landmarks", 10);

		for (Tag sectionTag : landmarkTag) {
			LandmarkSection section = LandmarkSection.fromTag((CompoundTag) sectionTag);
			this.landmarkSections.add(section);
		}
	}

	@Override
	public void writeToNbt(CompoundTag tag) {
		ListTag landmarkTag = tag.getList("landmarks", 10);

		for (LandmarkSection section : this.landmarkSections) {
			landmarkTag.add(section.toTag(new CompoundTag()));
		}

		tag.put("landmarks", landmarkTag);
	}

	public void add(LandmarkSection section) {
		if (!this.landmarkSections.contains(section)) {
			this.landmarkSections.add(section);
		}
	}

	public void remove(Landmark landmark) {
		this.landmarkSections.removeIf((section -> section.matches(landmark.getId())));
	}

	public Collection<LandmarkSection> getSections() {
		return this.landmarkSections;
	}

	public List<UUID> getIds() {
		return this.landmarkSections.stream().map(section -> section.parent).distinct().collect(Collectors.toList());
	}

	public static LandmarkChunkComponent of(Chunk chunk) {
		return LandmarkMod.CHUNK_COMPONENT.get(chunk);
	}

	public Chunk getChunk() {
		return this.chunk;
	}

	public interface LandmarkContainer extends ComponentV3 {
		void add(LandmarkSection section);
		void remove(PlayerLandmark landmark);
		Collection<LandmarkSection> getSections();
		UUID getMatches(ServerWorld world, Vec3d pos);
	}
}
