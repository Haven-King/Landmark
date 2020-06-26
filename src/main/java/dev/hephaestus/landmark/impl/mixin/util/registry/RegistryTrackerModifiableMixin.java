package dev.hephaestus.landmark.impl.mixin.util.registry;

import dev.hephaestus.landmark.impl.LandmarkMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.util.registry.RegistryTracker;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.world.dimension.DimensionType;

@Mixin(RegistryTracker.Modifiable.class)
public class RegistryTrackerModifiableMixin {
	@Inject(method = "<init>(Lnet/minecraft/util/registry/SimpleRegistry;)V", at = @At("TAIL"))
	private void captureDimensionRegistry(SimpleRegistry<DimensionType> registry, CallbackInfo ci) {
		LandmarkMod.DIMENSION_TYPE_REGISTRY = registry;
	}
}
