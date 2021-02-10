package dev.hephaestus.landmark.impl.client;

import dev.hephaestus.landmark.impl.LandmarkMod;
import dev.hephaestus.landmark.impl.LandmarkNetworking;
import dev.hephaestus.landmark.api.LandmarkCache;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.util.Rarity;

@Environment(EnvType.CLIENT)
public class LandmarkClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LandmarkRenderer.init();
        LandmarkNetworking.initClient();
        LandmarkHandler.init();

        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> {
            if (tintIndex == 1) {
                if (stack.getRarity() == Rarity.COMMON) {
                    return 0xFFFF0000;
                }

                //noinspection ConstantConditions
                return stack.getRarity().formatting.getColorValue();
            }

            return -1;
        }, LandmarkMod.COMMON_DEED, LandmarkMod.UNCOMMON_DEED, LandmarkMod.RARE_DEED, LandmarkMod.EPIC_DEED, LandmarkMod.CREATIVE_DEED);

        ClientChunkEvents.CHUNK_UNLOAD.register((world, chunk) -> {
            LandmarkCache.getInstance(world).remove(chunk.getPos());
        });
    }
}
