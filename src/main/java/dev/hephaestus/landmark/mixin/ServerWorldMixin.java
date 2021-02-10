package dev.hephaestus.landmark.mixin;

import dev.hephaestus.landmark.impl.LandmarkNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerWorld.class)
public class ServerWorldMixin {
    @Inject(method = "addPlayer", at = @At("TAIL"))
    private void sendLandmarksWithoutChunks(ServerPlayerEntity player, CallbackInfo ci) {
        LandmarkNetworking.send(player.getServerWorld(), player);
    }
}
