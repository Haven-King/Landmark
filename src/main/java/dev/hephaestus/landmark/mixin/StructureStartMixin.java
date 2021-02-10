package dev.hephaestus.landmark.mixin;

import dev.hephaestus.landmark.impl.LandmarkMod;
import dev.hephaestus.landmark.impl.util.Landmarks;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(StructureStart.class)
public abstract class StructureStartMixin {
	@Unique private static final Map<StructureStart<?>, Boolean> GENERATED = new ConcurrentHashMap<>();

	@Inject(method = "generateStructure", at = @At(value = "INVOKE", target = "Lnet/minecraft/structure/StructureStart;setBoundingBoxFromChildren()V", shift = At.Shift.AFTER))
	private void makeLandmark(StructureWorldAccess structureWorldAccess, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator, Random random, BlockBox blockBox, ChunkPos chunkPos, CallbackInfo ci) {
		//noinspection SuspiciousMethodCalls
		if (!GENERATED.containsKey(this)) {
			//noinspection ConstantConditions
			GENERATED.put((StructureStart<?>) (Object) this, true);
			//noinspection ConstantConditions
			LandmarkMod.EXECUTOR.execute(() -> Landmarks.of(structureWorldAccess.toServerWorld()).add((StructureStart<?>) (Object) this));
		}
	}
 }
