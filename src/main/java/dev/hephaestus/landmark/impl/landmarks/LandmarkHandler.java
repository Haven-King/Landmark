package dev.hephaestus.landmark.impl.landmarks;

import java.io.InputStreamReader;
import java.util.Collection;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.hephaestus.landmark.api.LandmarkType;
import dev.hephaestus.landmark.api.LandmarkTypeRegistry;
import dev.hephaestus.landmark.impl.LandmarkMod;
import io.netty.buffer.Unpooled;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.fabricmc.fabric.api.network.PacketContext;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;

public class LandmarkHandler {
	private static final Identifier LANDMARK_DISCOVERED_PACKET = LandmarkMod.id("packet", "discovered");

	@Environment(EnvType.CLIENT)
	public static void initClient() {
		ClientSidePacketRegistry.INSTANCE.register(LANDMARK_DISCOVERED_PACKET, LandmarkHandler::accept);
	}

	@Environment(EnvType.CLIENT)
	private static void accept(PacketContext context, PacketByteBuf buf) {
		String landmarkName = buf.readString();
		context.getTaskQueue().execute(() -> MinecraftClient.getInstance().inGameHud.setTitles(new LiteralText(landmarkName), null, 10, 70, 20));
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

				for (Identifier resource : resources) {
					JsonParser parser = new JsonParser();

					try {
						JsonElement jsonElement = parser.parse(new InputStreamReader(manager.getResource(resource).getInputStream()));

						Identifier id = new Identifier(
								resource.getNamespace(),
								resource.getPath().substring(
										resource.getPath().indexOf("landmarks/") + 10,
										resource.getPath().indexOf(".json")
								)
						);

						LandmarkTypeRegistry.register(LandmarkSerializer.deserialize(id, jsonElement));
						registered++;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				LandmarkMod.LOG.info("Registered " + registered + " landmarks");
			}
		});
	}

	public static void dispatch(ServerPlayerEntity player, LandmarkType landmarkType) {
		PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
		buf.writeString(LandmarkTracker.getLandmarkName(landmarkType, player.getServerWorld(), player.getBlockPos()));

		ServerSidePacketRegistry.INSTANCE.sendToPlayer(player, LANDMARK_DISCOVERED_PACKET, buf);
	}
}
