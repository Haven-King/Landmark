package dev.hephaestus.landmark.impl.landmarks;

import dev.hephaestus.landmark.impl.LandmarkMod;
import dev.hephaestus.landmark.impl.util.SyncedState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;

import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Queue;

public class LandmarkTracker extends SyncedState {
    private static final String KEY = "landmark_tracker";
    private static final Identifier LANDMARK_SYNC_ID = LandmarkMod.id("sync");

    private final HashMap<ChunkPos, Queue<Landmark.Section>> landmarkMap = new HashMap<>();

    public LandmarkTracker(MinecraftServer server) {
        super(KEY, LANDMARK_SYNC_ID, server);
    }

    public void add(Landmark landmark) {
        ChunkPos pos = new ChunkPos(landmark.getCenter());
        this.landmarkMap.computeIfAbsent(pos, (x) -> new PriorityQueue<>());
        landmark.makeSections(this.landmarkMap.get(pos));
    }

    public static LandmarkTracker get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(() -> new LandmarkTracker(world.getServer()), KEY);
    }

    @Override
    public void fromTag(CompoundTag tag) {

    }

    @Override
    protected void setTag(CompoundTag tag) {


        super.setTag(tag);
    }
}
