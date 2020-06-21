package dev.hephaestus.landmark.impl.mixin.entity;

import dev.hephaestus.landmark.api.LandmarkType;
import dev.hephaestus.landmark.api.LandmarkTypeRegistry;
import dev.hephaestus.landmark.impl.LandmarkMod;
import dev.hephaestus.landmark.impl.util.LandmarkHandler;
import dev.hephaestus.landmark.impl.util.Profiler;
import dev.hephaestus.landmark.impl.world.LandmarkTrackingComponent;
import dev.hephaestus.landmark.impl.world.chunk.LandmarkChunkComponent;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.UUID;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends LivingEntity {
	protected ServerPlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
		super(entityType, world);
	}

	@Shadow public abstract ServerWorld getServerWorld();

	@Unique
	private final HashMap<LandmarkType, Boolean> landmarkStatuses = new HashMap<>();

	@Inject(method = "playerTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/advancement/criterion/LocationArrivalCriterion;trigger(Lnet/minecraft/server/network/ServerPlayerEntity;)V"))
	private void checkLandmarks(CallbackInfo ci) {
		// TODO: Add some kind of delay before you can see the same message again
		LandmarkChunkComponent container = LandmarkMod.CHUNK_COMPONENT.get(this.getServerWorld().getChunk(this.getBlockPos()));
		UUID landmark = container.getMatches(this.getPos());

		Profiler.report(LandmarkMod.LOG);

		if (landmark != null) {
			PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
			buf.writeText(LandmarkTrackingComponent.of(this.getServerWorld()).getName(landmark));
			ServerSidePacketRegistry.INSTANCE.sendToPlayer((ServerPlayerEntity) (Object) this, LandmarkMod.LANDMARK_DISCOVERED_PACKET, buf);
		}

//		for (Identifier landmarkTypeId : LandmarkTypeRegistry.getRegistered()) {
//			LandmarkType landmarkType = LandmarkTypeRegistry.get(landmarkTypeId);
//
//			boolean isInLandmark = landmarkType.test((ServerPlayerEntity) (Object) this);
//
//			if (isInLandmark && !landmarkStatuses.getOrDefault(landmarkType, false)) {
//				LandmarkHandler.dispatch((ServerPlayerEntity) (Object) this, landmarkType);
//			}
//
//			this.landmarkStatuses.put(landmarkType, isInLandmark);
//		}
	}
}
