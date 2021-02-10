package dev.hephaestus.landmark.api;

import dev.hephaestus.landmark.impl.util.Landmark;
import dev.hephaestus.landmark.impl.util.Landmarks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.util.UUID;

public interface LandmarkHolder {
    Iterable<Landmark> getLandmarks();
    Iterable<Landmark> getLandmarks(ChunkPos pos);
    Iterable<Landmark> getLandmarks(BlockPos pos);
    Landmark getLandmark(UUID id);

    static LandmarkHolder getInstance(World world) {
        if (world.isClient) {
            return LandmarkCache.getInstance(world);
        } else {
            return Landmarks.of((ServerWorld) world);
        }
    }

    boolean contains(UUID landmark_id);
}
