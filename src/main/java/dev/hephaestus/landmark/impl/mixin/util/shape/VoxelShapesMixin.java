package dev.hephaestus.landmark.impl.mixin.util.shape;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.util.shape.VoxelShapes;

@Mixin(VoxelShapes.class)
public class VoxelShapesMixin {
//	@Redirect(method = "combine", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/function/BooleanBiFunction;apply(ZZ)Z", ordinal = 0))
//	private static boolean allowSubtraction(BooleanBiFunction function, boolean bl, boolean bl2) {
//		return !PlayerLandmark.tryingSubtraction && function.apply(bl, bl2);
//	}
}
