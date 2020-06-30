package dev.hephaestus.landmark.impl;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import dev.hephaestus.landmark.impl.item.DeedItem;
import dev.hephaestus.landmark.impl.item.EvictionNoticeItem;
import dev.hephaestus.landmark.impl.util.LandmarkHandler;
import dev.hephaestus.landmark.impl.names.NameGenerator;
import dev.hephaestus.landmark.impl.world.LandmarkTrackingComponent;
import dev.hephaestus.landmark.impl.world.chunk.LandmarkChunkComponent;
import nerdhub.cardinal.components.api.ComponentRegistry;
import nerdhub.cardinal.components.api.ComponentType;
import nerdhub.cardinal.components.api.event.ChunkComponentCallback;
import nerdhub.cardinal.components.api.event.WorldComponentCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.options.KeyBinding;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.world.dimension.DimensionType;
import net.minecraft.util.Identifier;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.Rarity;
import net.minecraft.util.registry.Registry;
import net.minecraft.item.ItemStack;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import org.lwjgl.glfw.GLFW;

public class LandmarkMod implements ModInitializer {
	public static final String MODID = "landmark";
	public static final String MOD_NAME = "Landmark";
	public static final Logger LOG = LogManager.getLogger(MOD_NAME);

	public static final Executor EXECUTOR = Executors.newCachedThreadPool((runnable) -> {
		Thread t = new Thread();
		t.setDaemon(true);
		return t;
	});

	public static final ItemGroup ITEM_GROUP = FabricItemGroupBuilder.create(id("items")).icon(() -> new ItemStack(LandmarkMod.COMMON_DEED)).build();

	public static final Item COMMON_DEED = new DeedItem(new Item.Settings().group(ITEM_GROUP).rarity(Rarity.COMMON), 4096);
	public static final Item UNCOMMON_DEED = new DeedItem(new Item.Settings().group(ITEM_GROUP).rarity(Rarity.UNCOMMON), 32768);
	public static final Item RARE_DEED = new DeedItem(new Item.Settings().group(ITEM_GROUP).rarity(Rarity.RARE), 262144);
	public static final Item CREATIVE_DEED = new DeedItem(new Item.Settings().group(ITEM_GROUP).rarity(Rarity.EPIC), Double.MAX_VALUE);

	public static final Item EVITION_NOTICE = new EvictionNoticeItem(new Item.Settings().group(ITEM_GROUP).rarity(Rarity.EPIC));

	public static Registry<DimensionType> DIMENSION_TYPE_REGISTRY = null;

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

		ChunkComponentCallback.EVENT.register(((chunk, componentContainer) -> componentContainer.put(CHUNK_COMPONENT, new LandmarkChunkComponent(chunk))));
		WorldComponentCallback.EVENT.register(((world, componentContainer) -> componentContainer.put(TRACKING_COMPONENT, new LandmarkTrackingComponent(world))));

		Registry.register(Registry.ITEM, id("common_deed"), COMMON_DEED);
		Registry.register(Registry.ITEM, id("uncommon_deed"), UNCOMMON_DEED);
		Registry.register(Registry.ITEM, id("rare_deed"), RARE_DEED);
		Registry.register(Registry.ITEM, id("creative_deed"), CREATIVE_DEED);
		Registry.register(Registry.ITEM, id("eviction_notice"), EVITION_NOTICE);
	}
}
