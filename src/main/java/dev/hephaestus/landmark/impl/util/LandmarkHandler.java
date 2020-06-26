package dev.hephaestus.landmark.impl.util;

import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
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
import dev.hephaestus.landmark.impl.names.NameGenerator;
import dev.hephaestus.landmark.impl.names.provider.NameComponentProvider;
import dev.hephaestus.landmark.impl.names.provider.types.Translatable;
import dev.hephaestus.landmark.impl.network.LandmarkNetworking;
import dev.hephaestus.landmark.impl.world.LandmarkTrackingComponent;
import dev.hephaestus.landmark.impl.world.chunk.LandmarkChunkComponent;
import io.netty.buffer.Unpooled;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.Pair;

import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;

public class LandmarkHandler {
	private final ServerPlayerEntity playerEntity;
	private final ConcurrentHashMap<UUID, Integer> landmarkStatuses = new ConcurrentHashMap<>();

	private final ConcurrentHashMap<Identifier, Boolean> otherLandmarkStatuses = new ConcurrentHashMap<>();

	public LandmarkHandler(ServerPlayerEntity playerEntity) {
		this.playerEntity = playerEntity;
	}

	public void tick() {
		LandmarkChunkComponent container = LandmarkMod.CHUNK_COMPONENT.get(this.playerEntity.getServerWorld().getChunk(this.playerEntity.getBlockPos()));

		double x = this.playerEntity.getPos().x, y = this.playerEntity.getPos().y, z = this.playerEntity.getPos().z;

		Text text = null;
		HashMap<UUID, Boolean> landmarks = new HashMap<>();

		for (Iterator<LandmarkType> it = LandmarkTypeRegistry.getRegistered(); it.hasNext(); ) {
			LandmarkType type = it.next();

			Pair<Integer, LandmarkType> testResults = type.test(null, playerEntity.getBlockPos(), playerEntity.getServerWorld());

			if (testResults.getLeft() >= 0 && !otherLandmarkStatuses.containsKey(type.getId())) {
				text = type.generateName();
				this.otherLandmarkStatuses.put(type.getId(), true);
			} else if (testResults.getLeft() < 0 && otherLandmarkStatuses.containsKey(type.getId())) {
				this.otherLandmarkStatuses.remove(type.getId());
			}
		}

		if (text == null) {
			for (LandmarkSection section : container.getSections()) {
				boolean bl = section.contains(x, y, z);
				landmarks.put(section.parent, landmarks.getOrDefault(section.parent, false) || bl);

				if (bl && text == null && !this.landmarkStatuses.containsKey(section.parent)) {
					text = LandmarkTrackingComponent.of(this.playerEntity.getServerWorld()).getName(section.parent);
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
		}

		if (text != null) {
			PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
			buf.writeText(text);
			ServerSidePacketRegistry.INSTANCE.sendToPlayer(this.playerEntity, LandmarkNetworking.ENTERED_LANDMARK, buf);
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
						LandmarkLocationPredicate predicate = LandmarkLocationPredicate.fromJson(jsonObject.get("location"));

						TextColor color = TextColor.fromFormatting(Formatting.WHITE);

						if (jsonObject.has("color")) {
							color = TextColor.parse(jsonObject.get("color").getAsString());
						}

						LandmarkType type = new LandmarkType(id, predicate, color);

						if (jsonObject.has("name_generator")) {
							JsonElement nameGenerator = jsonObject.get("name_generator");

							if (nameGenerator.isJsonPrimitive() && nameGenerator.getAsJsonPrimitive().isString()) {
								type.addNameGenerator(new Identifier(jsonObject.get("name_generator").getAsString()));
							} else if (nameGenerator.isJsonArray()) {
								for (JsonElement element : nameGenerator.getAsJsonArray()) {
									if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
										type.addNameGenerator(new Identifier(element.getAsString()));
									}
								}
							}
						}

						if (jsonObject.has("name")) {
							NameComponentProvider generator = NameGenerator.register(new Translatable(id, JsonHelper.getString(jsonObject, "name")));
							type.addNameGenerator(generator.getId());
						}

						LandmarkTypeRegistry.register(type);
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
