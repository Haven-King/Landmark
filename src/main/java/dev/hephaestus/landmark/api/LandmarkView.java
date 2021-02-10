package dev.hephaestus.landmark.api;

import net.minecraft.client.util.math.Vector3f;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;

import java.util.UUID;

public interface LandmarkView {
    UUID getId();

    Iterable<BlockBox> getBoxes();

    Iterable<ChunkPos> getChunks();

    Text getName();

    Vector3f getColor();

    boolean contains(Vec3i pos);

    boolean isOwnedBy(UUID playerId);

    default boolean isOwnedBy(PlayerEntity playerEntity) {
        return this.isOwnedBy(playerEntity.getUuid());
    }

    boolean isClaimed();

    int getVolume();

    BlockPos getCenter();
}
