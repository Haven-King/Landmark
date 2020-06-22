package dev.hephaestus.landmark.impl.mixin.entity;

import com.mojang.authlib.GameProfile;
import dev.hephaestus.landmark.impl.util.LandmarkHandler;
import dev.hephaestus.landmark.impl.world.LandmarkTrackingComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends LivingEntity {
	protected ServerPlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
		super(entityType, world);
		LandmarkTrackingComponent.of(world).syncWith((ServerPlayerEntity) (Object) this);
	}

	@Unique private LandmarkHandler landmarkHandler;

	@Inject(method = "<init>", at = @At("TAIL"))
	private void addLandmarkHandler(MinecraftServer server, ServerWorld world, GameProfile profile, ServerPlayerInteractionManager interactionManager, CallbackInfo ci) {
		landmarkHandler = new LandmarkHandler((ServerPlayerEntity) (Object) this);
	}

	@Inject(method = "playerTick", at = @At("TAIL"))
	private void checkLandmarks(CallbackInfo ci) {
		this.landmarkHandler.tick();
	}
}
