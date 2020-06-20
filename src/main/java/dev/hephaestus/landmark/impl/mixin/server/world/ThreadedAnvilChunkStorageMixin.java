package dev.hephaestus.landmark.impl.mixin.server.world;

import dev.hephaestus.landmark.impl.LandmarkMod;
import dev.hephaestus.landmark.impl.landmarks.Landmark;
import dev.hephaestus.landmark.impl.landmarks.LandmarkTracker;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(ThreadedAnvilChunkStorage.class)
public class ThreadedAnvilChunkStorageMixin {
    @Shadow @Final private ServerWorld world;

//    @Inject(method = "method_17227", at = @At("TAIL"))
//    private void buildLandmarks(ChunkHolder chunkHolder, Chunk protoChunk, CallbackInfoReturnable<Chunk> callbackInfoReturnable) {
//        Chunk chunk = callbackInfoReturnable.getReturnValue();
//        Collection<Landmark> landmarks = LandmarkTracker.get(world).get(chunk.getPos());
//        if (landmarks != null) {
//            for (Landmark landmark : landmarks) {
//                boolean allChunksLoaded = true;
//                for (ChunkPos pos : landmark.getChunks()) {
//                    allChunksLoaded &= world.isChunkLoaded(pos.x, pos.z);
//                }
//
//                if (allChunksLoaded) {
//                    landmark.makeSections(world);
//                }
//            }
//        }
//    }
}
