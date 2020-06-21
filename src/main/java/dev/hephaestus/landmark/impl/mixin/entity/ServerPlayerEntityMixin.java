package dev.hephaestus.landmark.impl.mixin.entity;

import com.mojang.authlib.GameProfile;
import dev.hephaestus.landmark.api.LandmarkType;
import dev.hephaestus.landmark.api.LandmarkTypeRegistry;
import dev.hephaestus.landmark.impl.LandmarkMod;
import dev.hephaestus.landmark.impl.landmarks.GeneratedLandmark;
import dev.hephaestus.landmark.impl.landmarks.Landmark;
import dev.hephaestus.landmark.impl.landmarks.LandmarkSection;
import dev.hephaestus.landmark.impl.landmarks.PlayerLandmark;
import dev.hephaestus.landmark.impl.names.NameGenerator;
import dev.hephaestus.landmark.impl.util.LandmarkHandler;
import dev.hephaestus.landmark.impl.util.Profiler;
import dev.hephaestus.landmark.impl.world.LandmarkTrackingComponent;
import dev.hephaestus.landmark.impl.world.chunk.LandmarkChunkComponent;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.StructureFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

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
