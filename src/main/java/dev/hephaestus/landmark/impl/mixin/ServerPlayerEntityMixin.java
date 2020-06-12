package dev.hephaestus.landmark.impl.mixin;

import java.util.HashMap;

import dev.hephaestus.landmark.api.LandmarkType;
import dev.hephaestus.landmark.api.LandmarkTypeRegistry;
import dev.hephaestus.landmark.impl.landmarks.LandmarkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {
	private final HashMap<LandmarkType, Boolean> landmarkStatuses = new HashMap<>();

	@Inject(method = "playerTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/advancement/criterion/LocationArrivalCriterion;trigger(Lnet/minecraft/server/network/ServerPlayerEntity;)V"))
	private void checkLandmarks(CallbackInfo ci) {
		for (Identifier landmarkTypeId : LandmarkTypeRegistry.getRegistered()) {
			LandmarkType landmarkType = LandmarkTypeRegistry.get(landmarkTypeId);

			boolean isInLandmark = landmarkType.test((ServerPlayerEntity) (Object) this);

			if (isInLandmark && !landmarkStatuses.getOrDefault(landmarkType, false)) {
				LandmarkHandler.dispatch((ServerPlayerEntity) (Object) this, landmarkType);
			}

			this.landmarkStatuses.put(landmarkType, isInLandmark);
		}
	}
}
