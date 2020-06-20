package dev.hephaestus.landmark.impl.world.chunk;

import dev.hephaestus.landmark.impl.landmarks.Landmark;
import dev.hephaestus.landmark.impl.landmarks.LandmarkTracker;
import nerdhub.cardinal.components.api.component.Component;
import nerdhub.cardinal.components.api.util.sync.ChunkSyncedComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Collection;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

public class LandmarkChunkComponent implements ChunkSyncedComponent<LandmarkChunkComponent.LandmarkContainer> {
    private Queue<Landmark.Section> landmarkSections = new ConcurrentLinkedDeque<>();
    private Chunk chunk;
    private World world;

    public LandmarkChunkComponent(Chunk chunk) {
        this.chunk = chunk;
        if (chunk instanceof WorldChunk) {
            this.world = ((WorldChunk) chunk).getWorld();
        }
    }

    @Override
    public void fromTag(CompoundTag tag) {
        this.landmarkSections = new PriorityQueue<>();
        ListTag landmarkTag = tag.getList("landmarks", 10);

        for (Tag sectionTag : landmarkTag) {
            Landmark.Section section = Landmark.Section.fromTag((CompoundTag) sectionTag);
            this.landmarkSections.add(section);

            if (this.world != null && this.world instanceof ServerWorld) {
                Landmark landmark = LandmarkTracker.get((ServerWorld) this.world).get(section.parent);
                landmark.add(section);
                landmark.makeSections((ServerWorld) world);
            }
        }
    }

    @Override
    public CompoundTag toTag(CompoundTag tag) {
        ListTag landmarkTag = tag.getList("landmarks", 10);

        for (Landmark.Section section : this.landmarkSections) {
            landmarkTag.add(section.toTag(new CompoundTag()));
        }

        tag.put("landmarks", landmarkTag);

        return tag;
    }

    public void add(Landmark.Section section) {
        if (!this.landmarkSections.contains(section)) {
            this.landmarkSections.add(section);
        }
    }

    public void remove(Landmark landmark) {
        this.landmarkSections.removeIf((section -> section.matches(landmark.uuid)));
    }

    public Collection<Landmark.Section> getSections() {
        return this.landmarkSections;
    }

    public UUID getMatches(Vec3d pos) {
        for (Landmark.Section section : this.landmarkSections) {
            if (section.contains(pos.x, pos.y, pos.z)) {
                return section.parent;
            }
        }

        return null;
    }

    @Override
    public Chunk getChunk() {
        return this.chunk;
    }

    public interface LandmarkContainer extends Component {
        void add(Landmark.Section section);
        void remove(Landmark landmark);
        Collection<Landmark.Section> getSections();
        UUID getMatches(ServerWorld world, Vec3d pos);
    }
}
