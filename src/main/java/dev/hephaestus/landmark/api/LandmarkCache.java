package dev.hephaestus.landmark.api;

import dev.hephaestus.landmark.impl.client.LandmarkCacheImpl;
import dev.hephaestus.landmark.impl.util.Landmark;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
public interface LandmarkCache extends LandmarkHolder {
    void cache(Landmark landmark);
    void remove(ChunkPos pos);
    void clear();

    boolean isInitialized();

    static LandmarkCache getInstance(World world) {
        if (world.isClient) {
            return LandmarkCacheImpl.getInstance(world.getRegistryKey());
        } else {
            throw new RuntimeException("Can't get LandmarkCache from server world");
        }
    }

    static LandmarkCache getInstance(RegistryKey<World> world) {
        return LandmarkCacheImpl.getInstance(world);
    }
}
