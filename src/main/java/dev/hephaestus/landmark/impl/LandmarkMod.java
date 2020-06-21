package dev.hephaestus.landmark.impl;

import dev.hephaestus.landmark.impl.item.DeedItem;
import dev.hephaestus.landmark.impl.util.LandmarkHandler;
import dev.hephaestus.landmark.impl.names.NameGenerator;
import dev.hephaestus.landmark.impl.world.LandmarkTrackingComponent;
import dev.hephaestus.landmark.impl.world.chunk.LandmarkChunkComponent;
import nerdhub.cardinal.components.api.ComponentRegistry;
import nerdhub.cardinal.components.api.ComponentType;
import nerdhub.cardinal.components.api.event.ChunkComponentCallback;
import nerdhub.cardinal.components.api.event.WorldComponentCallback;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.Rarity;
import net.minecraft.util.registry.Registry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.util.Identifier;

import net.fabricmc.api.ModInitializer;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class LandmarkMod implements ModInitializer {
	public static final String MODID = "landmark";
	public static final String MOD_NAME = "Landmark";
	public static final Logger LOG = LogManager.getLogger(MOD_NAME);
	public static final Identifier LANDMARK_DISCOVERED_PACKET = LandmarkMod.id("packet", "discovered");

	public static final Executor EXECUTOR = Executors.newFixedThreadPool(8);

	public static final Item COMMON_DEED = new DeedItem(new Item.Settings().group(ItemGroup.MISC).rarity(Rarity.COMMON), 4096);
	public static final Item UNCOMMON_DEED = new DeedItem(new Item.Settings().group(ItemGroup.MISC).rarity(Rarity.UNCOMMON), 32768);
	public static final Item RARE_DEED = new DeedItem(new Item.Settings().group(ItemGroup.MISC).rarity(Rarity.RARE), 262144);
	public static final Item CREATIVE_DEED = new DeedItem(new Item.Settings().group(ItemGroup.MISC).rarity(Rarity.EPIC), Integer.MAX_VALUE);

	public static final ComponentType<LandmarkChunkComponent> CHUNK_COMPONENT = ComponentRegistry.INSTANCE.registerIfAbsent(
			id("component", "chunk"),
			LandmarkChunkComponent.class
	);

	public static final ComponentType<LandmarkTrackingComponent> TRACKING_COMPONENT = ComponentRegistry.INSTANCE.registerIfAbsent(
			id("component", "tracking"),
			LandmarkTrackingComponent.class
	);

	public static Identifier id(String... path) {
		return new Identifier(MODID, String.join(".", path));
	}

	@Override
	public void onInitialize() {
		NameGenerator.init();
		LandmarkHandler.init();

		ServerSidePacketRegistry.INSTANCE.register(DeedItem.DEED_SAVE_PACKET_ID, DeedItem::saveName);

		ChunkComponentCallback.EVENT.register(((chunk, componentContainer) -> componentContainer.put(CHUNK_COMPONENT, new LandmarkChunkComponent(chunk))));
		WorldComponentCallback.EVENT.register(((world, componentContainer) -> componentContainer.put(TRACKING_COMPONENT, new LandmarkTrackingComponent(world))));

		Registry.register(Registry.ITEM, id("common_deed"), COMMON_DEED);
		Registry.register(Registry.ITEM, id("uncommon_deed"), UNCOMMON_DEED);
		Registry.register(Registry.ITEM, id("rare_deed"), RARE_DEED);
		Registry.register(Registry.ITEM, id("creative_deed"), CREATIVE_DEED);
	}
}
