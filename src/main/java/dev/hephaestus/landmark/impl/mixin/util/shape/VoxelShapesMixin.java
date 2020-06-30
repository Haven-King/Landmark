package dev.hephaestus.landmark.impl.mixin.util.shape;

import dev.hephaestus.landmark.impl.landmarks.PlayerLandmark;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.shape.VoxelShapes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(VoxelShapes.class)
public class VoxelShapesMixin {
//	@Redirect(method = "combine", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/function/BooleanBiFunction;apply(ZZ)Z", ordinal = 0))
//	private static boolean allowSubtraction(BooleanBiFunction function, boolean bl, boolean bl2) {
//		return !PlayerLandmark.tryingSubtraction && function.apply(bl, bl2);
//	}
}
