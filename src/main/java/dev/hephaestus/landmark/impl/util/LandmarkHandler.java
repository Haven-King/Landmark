package dev.hephaestus.landmark.impl.util;

import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.hephaestus.landmark.api.LandmarkType;
import dev.hephaestus.landmark.api.LandmarkTypeRegistry;
import dev.hephaestus.landmark.impl.LandmarkMod;
import dev.hephaestus.landmark.impl.landmarks.LandmarkSection;
import dev.hephaestus.landmark.impl.network.LandmarkNetworking;
import dev.hephaestus.landmark.impl.world.LandmarkTrackingComponent;
import dev.hephaestus.landmark.impl.world.chunk.LandmarkChunkComponent;
import io.netty.buffer.Unpooled;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;

public class LandmarkHandler {
	private final ServerPlayerEntity playerEntity;
	private final ConcurrentHashMap<UUID, Integer> landmarkStatuses = new ConcurrentHashMap<>();

	public LandmarkHandler(ServerPlayerEntity playerEntity) {
		this.playerEntity = playerEntity;
	}

	public void tick() {
		LandmarkChunkComponent container = LandmarkMod.CHUNK_COMPONENT.get(this.playerEntity.getServerWorld().getChunk(this.playerEntity.getBlockPos()));

		double x = this.playerEntity.getPos().x, y = this.playerEntity.getPos().y, z = this.playerEntity.getPos().z;

		UUID landmark = null;
		HashMap<UUID, Boolean> landmarks = new HashMap<>();

		for (LandmarkSection section : container.getSections()) {
			boolean bl = section.contains(x, y, z);
			landmarks.put(section.parent, landmarks.getOrDefault(section.parent, false) || bl);

			if (bl && landmark == null && !this.landmarkStatuses.containsKey(section.parent)) {
				landmark = section.parent;
				this.landmarkStatuses.put(section.parent, 200);
			}
		}

		for (Map.Entry<UUID, Integer> entry : this.landmarkStatuses.entrySet()) {
			if (!landmarks.getOrDefault(entry.getKey(), false)) {
				int i = entry.getValue() - 1;

				if (i <= 0) {
					this.landmarkStatuses.remove(entry.getKey());
				} else {
					this.landmarkStatuses.put(entry.getKey(), i);
				}
			}
		}

		if (landmark != null) {
			PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
			buf.writeText(LandmarkTrackingComponent.of(this.playerEntity.getServerWorld()).getName(landmark));
			ServerSidePacketRegistry.INSTANCE.sendToPlayer(this.playerEntity, LandmarkNetworking.ENTERED_LANDMARK, buf);
			this.landmarkStatuses.put(landmark, 200);
		}
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

						JsonObject jsonObject = jsonElement.getAsJsonObject();
						Identifier nameGenerator = new Identifier(jsonObject.get("name_generator").getAsString());
						LandmarkLocationPredicate predicate = LandmarkLocationPredicate.fromJson(jsonObject.get("location"));

						TextColor color = TextColor.fromFormatting(Formatting.WHITE);

						if (jsonObject.has("color")) {
							color = TextColor.parse(jsonObject.get("color").getAsString());
						}

						LandmarkTypeRegistry.register(new LandmarkType(id, nameGenerator, predicate, color));
						registered++;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				LandmarkMod.LOG.info("Registered " + registered + " landmarks");
			}
		});
	}
}
