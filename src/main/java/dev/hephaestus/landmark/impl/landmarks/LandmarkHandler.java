package dev.hephaestus.landmark.impl.landmarks;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.hephaestus.landmark.api.LandmarkRegistry;
import dev.hephaestus.landmark.impl.LandmarkMod;
import dev.hephaestus.landmark.impl.names.NameComponentProviderSerializer;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.io.InputStreamReader;
import java.util.Collection;

public class LandmarkHandler {
	private static final Identifier LANDMARK_DISCOVERED_PACKET = LandmarkMod.id("packet", "discovered");

	@Environment(EnvType.CLIENT)
	public static void initClient() {
		// TODO: Register landmark received packet
	}

	public static void init() {
		ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
			@Override
			public Identifier getFabricId() {
				return LandmarkMod.id("landmark", "loader");
			}

			@Override
			public void apply(ResourceManager manager) {
				Collection<Identifier> resources = manager.findResources("landmarks", (string -> string.endsWith(".json")));

				int registered = 0;
				for (Identifier id : resources) {
					JsonParser parser = new JsonParser();
					try {
						JsonElement jsonElement = parser.parse(new InputStreamReader(manager.getResource(id).getInputStream()));

						LandmarkRegistry.register(LandmarkSerializer.deserialize(jsonElement));
						registered++;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				LandmarkMod.LOG.info("Registered " + registered + " landmarks");
			}
		});
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
