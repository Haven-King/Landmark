package dev.hephaestus.landmark.impl.world;

import com.google.common.collect.ConcurrentHashMultiset;
import dev.hephaestus.landmark.impl.LandmarkClient;
import dev.hephaestus.landmark.impl.LandmarkMod;
import dev.hephaestus.landmark.impl.landmarks.Landmark;
import dev.hephaestus.landmark.impl.landmarks.PlayerLandmark;
import nerdhub.cardinal.components.api.util.sync.WorldSyncedComponent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.network.PacketContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LandmarkTrackingComponent implements WorldSyncedComponent {
    public static final Identifier LANDMARK_SYNC_ID = LandmarkMod.id("sync");

    private final World world;

    private ConcurrentHashMap<UUID, Landmark> landmarks = new ConcurrentHashMap<>();
    private ConcurrentHashMap<ChunkPos, ConcurrentHashMultiset<Landmark>> searcher = new ConcurrentHashMap<>();

    public LandmarkTrackingComponent(World world) {
        this.world = world;
    }

    public static void add(ServerWorld world, Landmark landmark) {
        LandmarkTrackingComponent tracker = of(world);
        tracker.landmarks.put(landmark.getId(), landmark);
        tracker.sync();
    }

    public static LandmarkTrackingComponent of(World world) {
        return LandmarkMod.TRACKING_COMPONENT.get(world);
    }

    public Landmark get(UUID uuid) {
        return this.landmarks.get(uuid);
    }

    @Override
    public void fromTag(CompoundTag tag) {
        this.landmarks = new ConcurrentHashMap<>();
        this.searcher = new ConcurrentHashMap<>();

        for (String key : tag.getKeys()) {
            Landmark landmark = Landmark.from(this.world, tag.getCompound(key));
            this.landmarks.put(UUID.fromString(key), landmark);

            for (ChunkPos pos : landmark.getChunks()) {
                this.searcher.computeIfAbsent(pos, (p) -> ConcurrentHashMultiset.create()).add(landmark);
            }
        }
    }

    @Override
    public CompoundTag toTag(CompoundTag tag) {
        for (Landmark landmark : this.landmarks.values()) {
            tag.put(landmark.getId().toString(), landmark.toTag(new CompoundTag()));
        }

        return tag;
    }

    public Text getName(UUID landmark) {
        return this.landmarks.get(landmark).getName();
    }

    @Environment(EnvType.CLIENT)
    public static void read(PacketContext context, PacketByteBuf buf) {
        CompoundTag tag = buf.readCompoundTag();

        context.getTaskQueue().execute(() -> {
            if (tag != null) {
                LandmarkClient.TRACKER.fromTag(tag);
            }
        });
    }

    public Collection<Landmark> get(ChunkPos pos) {
        return this.searcher.get(pos);
    }

    public void put(ChunkPos pos, PlayerLandmark landmark) {
        this.searcher.computeIfAbsent(pos, (p) -> ConcurrentHashMultiset.create()).add(landmark);
    }

    @Override
    public World getWorld() {
        return this.world;
    }
}
