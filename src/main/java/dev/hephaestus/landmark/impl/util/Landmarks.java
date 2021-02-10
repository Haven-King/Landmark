package dev.hephaestus.landmark.impl.util;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import dev.hephaestus.landmark.api.LandmarkHolder;
import dev.hephaestus.landmark.impl.LandmarkNetworking;
import dev.hephaestus.landmark.impl.Messages;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.text.Text;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.PersistentState;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Landmarks extends PersistentState implements LandmarkHolder {
    private static final String KEY = "Landmarks";

    private final ServerWorld world;
    private final Map<UUID, Landmark> landmarks = new ConcurrentHashMap<>();
    private final Multimap<ChunkPos, Landmark> landmarksByPosition = HashMultimap.create();
    private final Collection<Landmark> landmarksWithoutChunks = new HashSet<>();

    public final Executor executor;

    private Landmarks(ServerWorld world) {
        super(KEY);
        this.world = world;

        executor = Executors.newFixedThreadPool(1, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("Landmark-Worker-" + world.getRegistryKey());

            return t;
        });
    }

    public void add(StructureStart<?> structureStart) {
        UUID id = null;

        while (id == null || this.landmarks.containsKey(id)) {
            id = UUID.randomUUID();
        }

        Landmark landmark = Landmark.of(id, structureStart, this.world);

        if (landmark != null) {
            executor.execute(() -> {
                this.landmarks.put(landmark.getId(), landmark);

                Collection<ServerPlayerEntity> players = new HashSet<>();

                for (ChunkPos pos : landmark.getChunks()) {
                    this.landmarksByPosition.put(pos, landmark);
                    players.addAll(PlayerLookup.tracking(this.world, pos));
                }

                LandmarkNetworking.send(this.world, players, landmark);

                this.markDirty();
            });
        }
    }

    public TypedActionResult<@Nullable Text> add(UUID uuid, BlockBox box, int maxVolume) {
        if (this.contains(uuid)) {
            TypedActionResult<@Nullable Text> result = this.landmarks.get(uuid).add(box, maxVolume);

            if (result.getResult().isAccepted()) {
                this.markDirty();

                Landmark landmark = this.landmarks.get(uuid);
                Collection<ServerPlayerEntity> players = new HashSet<>();

                for (ChunkPos pos : landmark.getChunks()) {
                    this.landmarksByPosition.put(pos, landmark);
                    players.addAll(PlayerLookup.tracking(this.world, pos));
                }

                if (!landmark.getChunks().isEmpty()) {
                    this.landmarksWithoutChunks.remove(landmark);
                }

                LandmarkNetworking.send(this.world, players, landmark);
            }

            return result;
        }

        return TypedActionResult.fail(Messages.MISSING);
    }

    public boolean setName(UUID id, Text name) {
        if (this.contains(id)) {
            this.landmarks.get(id).setName(name);
            this.markDirty();
            return true;
        }

        return false;
    }

    public boolean setColor(UUID id, float r, float g, float b) {
        if (this.contains(id)) {
            this.landmarks.get(id).setColor(r, g, b);
            return true;
        }

        return false;
    }

    public void addOwner(UUID id, UUID player) {
        if (this.contains(id) && this.landmarks.get(id).addOwner(player)) {
            this.markDirty();
            LandmarkNetworking.send(this.world, this.landmarks.get(id));
        }
    }

    @Override
    public void fromTag(CompoundTag tag) {
        for (String key : tag.getKeys()) {
            UUID id = UUID.fromString(key);

            Landmark landmark = Landmark.fromTag(tag.getCompound(key));

            this.landmarks.put(id, landmark);

            for (ChunkPos pos : landmark.getChunks()) {
                this.landmarksByPosition.put(pos, landmark);
            }

            if (landmark.getChunks().isEmpty()) {
                this.landmarksWithoutChunks.add(landmark);
            }
        }
    }

    @Override
    public CompoundTag toTag(CompoundTag tag) {
        for (Map.Entry<UUID, Landmark> entry : this.landmarks.entrySet()) {
            Landmark landmark = entry.getValue();

            tag.put(entry.getKey().toString(), landmark.toTag());
        }

        return tag;
    }

    public static Landmarks of(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(() -> new Landmarks(world), KEY);
    }

    @Override
    public Collection<Landmark> getLandmarks() {
        return this.landmarks.values();
    }

    public Collection<Landmark> getLandmarks(ChunkPos pos) {
        return landmarksByPosition.get(pos);
    }

    public Collection<Landmark> getLandmarks(BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);

        Collection<Landmark> landmarks = new ArrayList<>();

        for (Landmark landmark : this.landmarksByPosition.get(chunkPos)) {
            if (landmark.contains(pos)) landmarks.add(landmark);
        }

        return landmarks;
    }

    @Override
    public Landmark getLandmark(UUID id) {
        return this.landmarks.get(id);
    }

    public boolean contains(UUID id) {
        return this.landmarks.containsKey(id);
    }

    public Landmark newLandmark(UUID owner) {
        UUID id = null;

        while (id == null || this.landmarks.containsKey(id)) {
            id = UUID.randomUUID();
        }

        Landmark landmark = new Landmark(id, owner);

        this.landmarksWithoutChunks.add(landmark);

        LandmarkNetworking.send(this.world, this.world.getPlayers(), landmark);

        this.landmarks.put(id, landmark);
        this.markDirty();

        return landmark;
    }

    public Collection<Landmark> getLandmarksWithoutChunks() {
        return this.landmarksWithoutChunks;
    }
}
