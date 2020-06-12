package dev.hephaestus.landmark.impl.landmarks;

import dev.hephaestus.landmark.api.LandmarkType;
import dev.hephaestus.landmark.impl.names.NameGenerator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import java.util.HashMap;
import java.util.Map;

public class LandmarkTracker extends PersistentState {
	private static final String ID = "landmarks";

	private final HashMap<RegistryKey<DimensionType>, HashMap<BlockPos, String>> landmarkNames = new HashMap<>();

	public LandmarkTracker() {
		super(ID);
	}

	@Override
	public void fromTag(CompoundTag tag) {
		CompoundTag landmarkTag = tag.getCompound(ID);

		for (String key : landmarkTag.getKeys()) {
			CompoundTag dimensionTag = landmarkTag.getCompound(key);
			RegistryKey<DimensionType> dimension = RegistryKey.of(Registry.DIMENSION_TYPE_KEY, new Identifier(key));

			for (String blockPos : dimensionTag.getKeys()) {
				this.landmarkNames.computeIfAbsent(dimension, (k) -> new HashMap<>()).put(
					BlockPos.fromLong(Long.parseLong(blockPos)),
					dimensionTag.getString(blockPos)
				);
			}
		}
	}

	@Override
	public CompoundTag toTag(CompoundTag tag) {
		CompoundTag landmarkTag = new CompoundTag();

		for (RegistryKey<DimensionType> key : landmarkNames.keySet()) {
			CompoundTag dimensionTag = new CompoundTag();

			for (Map.Entry<BlockPos, String> entry : landmarkNames.get(key).entrySet()) {
				dimensionTag.putString(String.valueOf(entry.getKey().asLong()), entry.getValue());
			}

			landmarkTag.put(key.getValue().toString(), dimensionTag);
		}

		tag.put(ID, landmarkTag);

		return tag;
	}

	private static LandmarkTracker get(ServerWorld world) {
		ServerWorld overworld = world.getServer().getWorld(World.OVERWORLD);

		if (overworld == null) {
			throw new IllegalStateException("Overworld doesn't exist!");
		}


		return overworld.getPersistentStateManager().getOrCreate(LandmarkTracker::new, ID);
	}

	public static String getLandmarkName(LandmarkType landmarkType, ServerWorld world, BlockPos pos) {
		LandmarkTracker tracker = get(world);

		BlockPos center = world.locateStructure(landmarkType.getFeature(), pos, 300, false);

		return tracker.landmarkNames.
				computeIfAbsent(world.getDimensionRegistryKey(), key -> new HashMap<>()).
				computeIfAbsent(center, key -> {
					tracker.markDirty();
					return NameGenerator.generate(landmarkType.getNameGeneratorId());
				});
	}
}
