package dev.hephaestus.landmark.impl;

import dev.hephaestus.landmark.api.Landmark;
import dev.hephaestus.landmark.api.LandmarkRegistry;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class LandmarkHandler {
	private static final Identifier LANDMARK_DISCOVERED_PACKET = LandmarkMod.id("packet", "discovered");

	@Environment(EnvType.CLIENT)
	public static void initClient() {
		// TODO: Register landmark received packet
	}

	public static void init() {
		// TODO: Load landmark definitions
	}

	public static void trigger(ServerPlayerEntity player) {
		// TODO: Should be called every second on each ServerPlayerEntity (After each call to Criteria.LOCATION.trigger)
		for (Identifier id : LandmarkRegistry.getRegistered()) {
			if (LandmarkRegistry.get(id).test(player)) {
				dispatch(player, id);
			}
		}
	}

	private static void dispatch(ServerPlayerEntity player, Identifier id) {
		PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
		buf.writeString(LandmarkTracker.getLandmarkName(id, player.getServerWorld(), player.getBlockPos()));

		ServerSidePacketRegistry.INSTANCE.sendToPlayer(player, LANDMARK_DISCOVERED_PACKET, buf);
	}
}
