package dev.hephaestus.landmark.impl.network;

import dev.hephaestus.landmark.impl.LandmarkMod;
import dev.hephaestus.landmark.impl.client.gui.EditScreen;
import dev.hephaestus.landmark.impl.client.gui.DeletionScreen;
import dev.hephaestus.landmark.impl.client.gui.ClaimScreen;
import dev.hephaestus.landmark.impl.client.NameRenderer;
import dev.hephaestus.landmark.impl.item.DeedItem;
import dev.hephaestus.landmark.impl.landmarks.Landmark;
import dev.hephaestus.landmark.impl.world.LandmarkTrackingComponent;

import net.minecraft.util.Identifier;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;

public class LandmarkNetworking implements ModInitializer, ClientModInitializer {
	public static final Identifier SAVE_LANDMARK_NAME = packetId("save", "name");

	public static final Identifier OPEN_EDIT_SCREEN = packetId("open_screen", "edit");
	public static final Identifier OPEN_CLAIM_SCREEN = packetId("open_screen", "claim");
	public static final Identifier OPEN_DELETION_SCREEN = packetId("open_screen", "delete");

	public static final Identifier TRACKER_SYNC = packetId("tracker", "sync");
	public static final Identifier TRACKER_NEW_LANDMARK = packetId("tracker", "new_landmark");
	public static final Identifier TRACKER_CLAIM_LANDMARK = packetId("tracker", "claim_landmark");
	public static final Identifier TRACKER_DELETE_LANDMARK = packetId("tracker", "delete");

	public static final Identifier TOGGLE_DELETE_MODE = packetId("deed", "delete_mode", "toggle");

	public static final Identifier ENTERED_LANDMARK = packetId("entered");

	@Override
	public void onInitialize() {
		ServerSidePacketRegistry.INSTANCE.register(SAVE_LANDMARK_NAME, Landmark::saveName);
		ServerSidePacketRegistry.INSTANCE.register(TRACKER_DELETE_LANDMARK, LandmarkTrackingComponent::delete);
		ServerSidePacketRegistry.INSTANCE.register(TRACKER_CLAIM_LANDMARK, LandmarkTrackingComponent::claim);
		ServerSidePacketRegistry.INSTANCE.register(TRACKER_NEW_LANDMARK, LandmarkTrackingComponent::newLandmark);
		ServerSidePacketRegistry.INSTANCE.register(TOGGLE_DELETE_MODE, DeedItem::toggleDeleteMode);
	}

	@Override
	public void onInitializeClient() {
		ClientSidePacketRegistry.INSTANCE.register(OPEN_EDIT_SCREEN, EditScreen::new);
		ClientSidePacketRegistry.INSTANCE.register(OPEN_DELETION_SCREEN, DeletionScreen::new);
		ClientSidePacketRegistry.INSTANCE.register(OPEN_CLAIM_SCREEN, ClaimScreen::new);
		ClientSidePacketRegistry.INSTANCE.register(TRACKER_SYNC, LandmarkTrackingComponent::read);
		ClientSidePacketRegistry.INSTANCE.register(ENTERED_LANDMARK, NameRenderer::accept);
	}

	private static Identifier packetId(String... args) {
		return LandmarkMod.id("packet", String.join(".", args));
	}
}
