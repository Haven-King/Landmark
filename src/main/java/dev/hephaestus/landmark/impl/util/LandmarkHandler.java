package dev.hephaestus.landmark.impl.util;

import java.io.InputStreamReader;
import java.util.Collection;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.hephaestus.landmark.api.LandmarkType;
import dev.hephaestus.landmark.api.LandmarkTypeRegistry;
import dev.hephaestus.landmark.impl.LandmarkMod;
import dev.hephaestus.landmark.impl.landmarks.GeneratedLandmark;
import dev.hephaestus.landmark.impl.landmarks.Landmark;
import dev.hephaestus.landmark.impl.landmarks.PlayerLandmark;
import dev.hephaestus.landmark.impl.world.LandmarkTrackingComponent;
import io.netty.buffer.Unpooled;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;

public class LandmarkHandler {
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

						LandmarkTypeRegistry.register(new LandmarkType(id, nameGenerator, predicate));
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
		Landmark landmark = new GeneratedLandmark(player.getServerWorld(), landmarkType);
		LandmarkTrackingComponent.add(player.getServerWorld(), landmark);

		// TODO: Have generated landmarks saved in the same way as custom ones.
		buf.writeText(landmark.getName());

		ServerSidePacketRegistry.INSTANCE.sendToPlayer(player, LandmarkMod.LANDMARK_DISCOVERED_PACKET, buf);
	}
}
