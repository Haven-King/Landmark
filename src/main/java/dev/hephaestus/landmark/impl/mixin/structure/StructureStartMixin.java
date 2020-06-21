package dev.hephaestus.landmark.impl.mixin.structure;

import com.google.common.collect.ImmutableList;
import dev.hephaestus.landmark.api.LandmarkTypeRegistry;
import dev.hephaestus.landmark.impl.LandmarkMod;
import dev.hephaestus.landmark.impl.landmarks.GeneratedLandmark;
import dev.hephaestus.landmark.impl.landmarks.Landmark;
import dev.hephaestus.landmark.impl.landmarks.PlayerLandmark;
import dev.hephaestus.landmark.impl.landmarks.LandmarkSection;
import dev.hephaestus.landmark.impl.world.LandmarkTrackingComponent;
import dev.hephaestus.landmark.impl.util.Profiler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(StructureStart.class)
public class StructureStartMixin {
    @Shadow @Final protected List<StructurePiece> children;

    @Shadow @Final private int chunkX;

    @Shadow @Final private int chunkZ;

    @Inject(method = "generateStructure", at = @At("TAIL"))
    private void makeLandmark(ServerWorldAccess serverWorldAccess, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator, Random random, BlockBox blockBox, ChunkPos chunkPos, CallbackInfo ci) {
        ServerWorld world;

        if (serverWorldAccess instanceof ServerWorld) {
            world = (ServerWorld) serverWorldAccess;
        } else if (serverWorldAccess instanceof ChunkRegion) {
            world = ((ChunkRegion) serverWorldAccess).getWorld();
        } else {
            world = null;
        }

        ImmutableList<StructurePiece> list = ImmutableList.copyOf(this.children);
        LandmarkMod.EXECUTOR.execute(() -> {
            if (world != null) {
                Profiler.push("newLandmark");
                GeneratedLandmark landmark = new GeneratedLandmark(world, new LiteralText("TEST"));
                LandmarkTrackingComponent.add(world, landmark);
                Profiler.pop(false);

                for (StructurePiece structurePiece : list) {
                    Profiler.push("addSection");
                    LandmarkSection section = new LandmarkSection(
                            landmark.getId(),
                            structurePiece.getBoundingBox()
                    );

                    Profiler.push("addToLandmark");
                    landmark.add(section);
                    Profiler.pop(false);
                    Profiler.pop(false);
                }

                world.getServer().submit(() -> GeneratedLandmark.resolve(landmark));
            }
        });
    }

//    @Inject(method = "generateStructure", at = @At(value = "INVOKE", target = "Lnet/minecraft/structure/StructurePiece;getBoundingBox()Lnet/minecraft/util/math/BlockBox;"), locals = LocalCapture.CAPTURE_FAILHARD)
//    private void makeLandmark(ServerWorldAccess serverWorldAccess, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator, Random random, BlockBox blockBox, ChunkPos chunkPos, CallbackInfo ci, List<?> var7, BlockBox box, Vec3i vec, BlockPos pos, Iterator<StructurePiece> iterator, StructurePiece structurePiece) {
//        ServerWorld world;
//
//        if (serverWorldAccess instanceof ServerWorld) {
//            world = (ServerWorld) serverWorldAccess;
//        } else if (serverWorldAccess instanceof ChunkRegion) {
//            world = ((ChunkRegion) serverWorldAccess).getWorld();
//        } else {
//            LandmarkMod.LOG.error("OOPS");
//        }
//    }
}
