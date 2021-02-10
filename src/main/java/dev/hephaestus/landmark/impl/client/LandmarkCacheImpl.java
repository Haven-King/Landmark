package dev.hephaestus.landmark.impl.client;

import dev.hephaestus.landmark.api.LandmarkCache;
import dev.hephaestus.landmark.impl.util.Landmark;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LandmarkCacheImpl implements LandmarkCache {
    private static final Map<RegistryKey<World>, LandmarkCache> CACHES = new ConcurrentHashMap<>();

    private final Map<ChunkPos, Collection<Landmark>> landmarks = new ConcurrentHashMap<>();
    private final Map<UUID, Landmark> landmarksByID = new ConcurrentHashMap<>();

    private boolean initialized = false;

    public static LandmarkCache getInstance(RegistryKey<World> world) {
        return CACHES.computeIfAbsent(world, w -> new LandmarkCacheImpl());
    }

    @Override
    public void cache(Landmark landmark) {
        this.landmarksByID.put(landmark.getId(), landmark);

        for (ChunkPos pos : landmark.getChunks()) {
            Collection<Landmark> collection = this.landmarks.computeIfAbsent(pos, p -> new HashSet<>());
            collection.removeIf(l -> l.getId().equals(landmark.getId()));
            collection.add(landmark);
        }
    }

    @Override
    public void remove(ChunkPos pos) {
        this.landmarks.remove(pos);
    }

    @Override
    public void clear() {
        this.landmarks.clear();
        this.landmarksByID.clear();
    }

    @Override
    public boolean isInitialized() {
        return this.initialized;
    }

    @Override
    public Iterable<Landmark> getLandmarks() {
        return this.landmarksByID.values();
    }

    @Override
    public Iterable<Landmark> getLandmarks(ChunkPos pos) {
        return this.landmarks.getOrDefault(pos, Collections.emptyList());
    }

    @Override
    public Iterable<Landmark> getLandmarks(BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);

        Collection<Landmark> landmarks = new ArrayList<>();

        for (Landmark landmark : this.landmarks.getOrDefault(chunkPos, Collections.emptyList())) {
            if (landmark.contains(pos)) landmarks.add(landmark);
        }

        return landmarks;
    }

    @Override
    public Landmark getLandmark(UUID id) {
        return this.landmarksByID.get(id);
    }

    @Override
    public boolean contains(UUID id) {
        return this.landmarksByID.containsKey(id);
    }

    public void initialize() {
        this.initialized = true;
    }
}
