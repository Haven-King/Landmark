package dev.hephaestus.landmark.impl.landmarks;

import com.google.common.collect.ConcurrentHashMultiset;
import dev.hephaestus.landmark.impl.LandmarkClient;
import dev.hephaestus.landmark.impl.LandmarkMod;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.network.PacketContext;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Consumer;

public class LandmarkTracker extends PersistentState implements Iterable<Landmark> {
    private static final String KEY = "landmark_tracker";
    public static final Identifier LANDMARK_SYNC_ID = LandmarkMod.id("sync");

    private final MinecraftServer server;
    private CompoundTag tag;

    private ConcurrentHashMap<UUID, Landmark> landmarks = new ConcurrentHashMap<>();
    private ConcurrentHashMap<ChunkPos, ConcurrentHashMultiset<Landmark>> searcher = new ConcurrentHashMap<>();

    public LandmarkTracker(MinecraftServer server) {
        super(KEY);
        this.server = server;
    }

    public static void add(ServerWorld world, Landmark landmark) {
        LandmarkTracker tracker = get(world);
        tracker.landmarks.put(landmark.uuid, landmark);
        landmark.makeSections(world);
        tracker.markDirty();
//        tracker.sync();
    }

    public static boolean add(ServerWorld world, UUID uuid, VoxelShape shape, float maxVolume) {
        LandmarkTracker tracker = get(world);
        Landmark landmark = tracker.get(uuid);
        boolean success = landmark.add(shape, maxVolume);

        if (success) {
            landmark.makeSections(world);
            tracker.markDirty();
//            tracker.sync();
        }

        return success;
    }

    public static LandmarkTracker get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(() -> new LandmarkTracker(world.getServer()), KEY);
    }

    public Landmark get(UUID uuid) {
        return this.landmarks.get(uuid);
    }

    @Override
    public void fromTag(CompoundTag tag) {
        this.landmarks = new ConcurrentHashMap<>();
        this.searcher = new ConcurrentHashMap<>();

        for (String key : tag.getKeys()) {
            Landmark landmark = Landmark.fromTag(tag.getCompound(key));
            this.landmarks.put(UUID.fromString(key), landmark);

            for (ChunkPos pos : landmark.getChunks()) {
                this.searcher.computeIfAbsent(pos, (p) -> ConcurrentHashMultiset.create()).add(landmark);
            }
        }
    }

    @Override
    public CompoundTag toTag(CompoundTag tag) {
        if (this.server != null) {
            for (Landmark landmark : this.landmarks.values()) {
                tag.put(landmark.uuid.toString(), landmark.toTag(new CompoundTag()));
            }
        }

        return tag;
    }

    public Text getName(UUID landmark) {
        return this.landmarks.get(landmark).getName();
    }

//    private void sync() {
//        if (this.server != null) {
//            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
//            buf.writeCompoundTag(this.toTag(new CompoundTag()));
//
//            for (ServerPlayerEntity player : this.server.getPlayerManager().getPlayerList()) {
//                ServerSidePacketRegistry.INSTANCE.sendToPlayer(player, LANDMARK_SYNC_ID, buf);
//            }
//        }
//    }

    @Environment(EnvType.CLIENT)
    public static void read(PacketContext context, PacketByteBuf buf) {
        CompoundTag tag = buf.readCompoundTag();

        context.getTaskQueue().execute(() -> {
            if (tag != null) {
                LandmarkClient.TRACKER.fromTag(tag);
            }
        });
    }

    @Override
    public Iterator<Landmark> iterator() {
        return this.landmarks.values().iterator();
    }

    @Override
    public void forEach(Consumer<? super Landmark> consumer) {
        this.landmarks.values().forEach(consumer);
    }

    public Collection<Landmark> get(ChunkPos pos) {
        return this.searcher.get(pos);
    }

    public void put(ChunkPos pos, Landmark landmark) {
        this.searcher.computeIfAbsent(pos, (p) -> ConcurrentHashMultiset.create()).add(landmark);
    }
}
