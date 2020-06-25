package dev.hephaestus.landmark.impl.landmarks;

import java.util.*;

import dev.hephaestus.landmark.impl.LandmarkMod;
import dev.hephaestus.landmark.impl.util.shape.VoxelShapeSerializer;
import dev.hephaestus.landmark.impl.world.LandmarkTrackingComponent;
import dev.hephaestus.landmark.impl.world.chunk.LandmarkChunkComponent;

import net.minecraft.nbt.*;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;

public class PlayerLandmark extends Landmark {
	private VoxelShape shape;
	private double volume;

	public PlayerLandmark(World world) {
		this(world, (MutableText) LiteralText.EMPTY);
	}

	public PlayerLandmark(World world, MutableText name) {
		super(world, UUID.randomUUID(), name);
	}

	public PlayerLandmark(World world, UUID id) {
		super(world, id, (MutableText) LiteralText.EMPTY);
	}

	@Override
	public CompoundTag toTag(CompoundTag tag) {
		tag.putString("type", "player");

		if (this.shape != null) {
			tag.put("shape", VoxelShapeSerializer.INSTANCE.toTag(new CompoundTag(), this.shape));
		}

		return super.toTag(tag);
	}

	@Override
	public Landmark fromTag(World world, CompoundTag tag) {
		super.fromTag(world, tag);

		this.volume = tag.getDouble("volume");

		this.shape = VoxelShapeSerializer.INSTANCE.fromTag(tag.getCompound("shape"));
		this.makeSections();

		return this;
	}

	public int add(LandmarkSection section, double maxVolume, boolean checks) {
		VoxelShape added = VoxelShapes.cuboid(
				section.minX,
				section.minY,
				section.minZ,
				section.maxX,
				section.maxY,
				section.maxZ
		);

		VoxelShape comparison = VoxelShapes.cuboid(
				section.minX - 1,
				section.minY - 1,
				section.minZ - 1,
				section.maxX + 1,
				section.maxY + 1,
				section.maxZ + 1
		);

		if (!checks || (this.shape != null && !VoxelShapes.matchesAnywhere(comparison, this.shape, BooleanBiFunction.AND))) {
			return 1;
		}

		VoxelShape newShape = this.shape == null ? added : VoxelShapes.union(this.shape, added);

		List<Double> volumes = new LinkedList<>();
		newShape.forEachBox((x1, y1, z1, x2, y2, z2) -> volumes.add((x2 - x1) * (y2 - y1) * (z2 - z1)));

		double volume = 0D;

		for (double d : volumes) {
			volume += d;
		}

		if (volume <= maxVolume) {
			this.shape = newShape/*.simplify()*/;
			this.volume = volume;
			return 0;
		}

		return 2;
	}

	public boolean add(LandmarkSection section) {
		return this.add(section, Double.MAX_VALUE, false) == 0;
	}

	public void makeSections() {
		if (this.shape != null) {
			for (ChunkPos pos : chunks) {
				LandmarkChunkComponent component = LandmarkMod.CHUNK_COMPONENT.get(this.getWorld().getChunk(pos.x, pos.z));
				component.remove(this);
			}

			this.shape.forEachBox(((minX, minY, minZ, maxX, maxY, maxZ) -> {
				LandmarkSection section = new LandmarkSection(this.getId(), minX, minY, minZ, maxX, maxY, maxZ);
				Collection<ChunkPos> chunks = section.getChunks();
				this.chunks.addAll(chunks);

				for (ChunkPos pos : chunks) {
					LandmarkChunkComponent component = LandmarkMod.CHUNK_COMPONENT.get(this.getWorld().getChunk(pos.x, pos.z));
					component.add(section);
				}
			}));

			LandmarkTrackingComponent tracker = LandmarkTrackingComponent.of(this.getWorld());

			for (ChunkPos pos : chunks) {
				tracker.put(pos, this);
				LandmarkChunkComponent component = LandmarkMod.CHUNK_COMPONENT.get(this.getWorld().getChunk(pos.x, pos.z));
				component.sync();
			}

			tracker.sync();
		}
	}

	public double volume() {
		return this.volume;
	}
}
