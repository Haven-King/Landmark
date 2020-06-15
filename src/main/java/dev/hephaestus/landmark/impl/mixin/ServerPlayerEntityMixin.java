package dev.hephaestus.landmark.impl.mixin;

import com.mojang.authlib.GameProfile;
import dev.hephaestus.landmark.api.LandmarkType;
import dev.hephaestus.landmark.api.LandmarkTypeRegistry;
import dev.hephaestus.landmark.impl.LandmarkMod;
import dev.hephaestus.landmark.impl.landmarks.CustomLandmarkTracker;
import dev.hephaestus.landmark.impl.landmarks.LandmarkHandler;
import dev.hephaestus.landmark.impl.landmarks.LandmarkNameTracker;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends LivingEntity {
	protected ServerPlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
		super(entityType, world);
	}

	@Shadow public abstract ServerWorld getServerWorld();

	private final HashMap<LandmarkType, Boolean> landmarkStatuses = new HashMap<>();
	private final HashMap<BlockPos, Boolean> customLandmarkStatuses = new HashMap<>();

	@Inject(method = "playerTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/advancement/criterion/LocationArrivalCriterion;trigger(Lnet/minecraft/server/network/ServerPlayerEntity;)V"))
	private void checkLandmarks(CallbackInfo ci) {
		// TODO: Add some kind of delay before you can see the same message again
		CustomLandmarkTracker tracker = CustomLandmarkTracker.get(getServerWorld());
		for (BlockPos pos : tracker.getAll()) {
			boolean overlaps = false;
			for (Box box : tracker.get(pos).getBoundingBoxes()) {
				double x = getX(), y = getY(), z = getZ();
				overlaps = x > box.minX && x < box.maxX && y > box.minY && y < box.maxY && z > box.minZ && z < box.maxZ;
				if (overlaps) break;
			}

			if (overlaps && !this.customLandmarkStatuses.getOrDefault(pos, false)) {
				PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
				Text text = LandmarkNameTracker.getLandmarkName(getServerWorld(), pos);
				buf.writeText(text);

				ServerSidePacketRegistry.INSTANCE.sendToPlayer((ServerPlayerEntity) (Object) this, LandmarkMod.LANDMARK_DISCOVERED_PACKET, buf);
			}

			this.customLandmarkStatuses.put(pos, overlaps);
		}

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
