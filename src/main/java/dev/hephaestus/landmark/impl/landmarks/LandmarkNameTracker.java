package dev.hephaestus.landmark.impl.landmarks;

import dev.hephaestus.landmark.api.LandmarkType;
import dev.hephaestus.landmark.impl.names.NameGenerator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;

public class LandmarkNameTracker extends PersistentState {
	private static final String ID = "landmarks";

	private final HashMap<BlockPos, Text> landmarkNames = new HashMap<>();

	public LandmarkNameTracker() {
		super(ID);
	}

	@Override
	public void fromTag(CompoundTag tag) {
		CompoundTag landmarkTag = tag.getCompound(ID);

		for (String blockPos : landmarkTag.getKeys()) {
			this.landmarkNames.put(
					BlockPos.fromLong(Long.parseLong(blockPos)),
					Text.Serializer.fromJson(landmarkTag.getString(blockPos))
			);
		}
	}

	@Override
	public CompoundTag toTag(CompoundTag tag) {
		CompoundTag landmarkTag = new CompoundTag();

		for (Map.Entry<BlockPos, Text> entry : landmarkNames.entrySet()) {
			landmarkTag.putString(String.valueOf(entry.getKey().asLong()), Text.Serializer.toJson(entry.getValue()));
		}

		tag.put(ID, landmarkTag);

		return tag;
	}

	private static LandmarkNameTracker get(ServerWorld world) {
		return world.getPersistentStateManager().getOrCreate(LandmarkNameTracker::new, ID);
	}

	public static boolean addCustomLandmark(ServerWorld world, BlockPos pos, Text name) {
		LandmarkNameTracker tracker = get(world);

		if (tracker.landmarkNames.containsKey(pos)) {
			return false;
		} else {
			tracker.markDirty();
			tracker.landmarkNames.put(pos, name);
			return true;
		}
	}

	public static Text getLandmarkName(ServerWorld world, BlockPos pos) {
		LandmarkNameTracker tracker = get(world);
		return tracker.landmarkNames.getOrDefault(pos, new LiteralText(""));
	}

	public static Text getLandmarkName(LandmarkType landmarkType, ServerWorld world, BlockPos pos) {
		LandmarkNameTracker tracker = get(world);

		BlockPos center = world.locateStructure(landmarkType.getFeature(), pos, 300, false);

		return tracker.landmarkNames
				.computeIfAbsent(center, key -> {
					tracker.markDirty();
					return NameGenerator.generate(landmarkType.getNameGeneratorId());
				});
	}
}
