package dev.hephaestus.landmark.impl;

import dev.hephaestus.landmark.impl.world.LandmarkTrackingComponent;
import me.sargunvohra.mcmods.autoconfig1u.AutoConfig;
import me.sargunvohra.mcmods.autoconfig1u.serializer.JanksonConfigSerializer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public class LandmarkClient implements ClientModInitializer {
	public static final LandmarkTrackingComponent TRACKER = new LandmarkTrackingComponent(null);
	public static LandmarkConfig CONFIG;
	//public static KeyBinding DELETE_MODE;
	//private static boolean wasPressed = false;

	@Override
	@Environment(EnvType.CLIENT)
	public void onInitializeClient() {
		AutoConfig.register(LandmarkConfig.class, JanksonConfigSerializer::new);
		CONFIG = AutoConfig.getConfigHolder(LandmarkConfig.class).getConfig();

		//DELETE_MODE = KeyBindingHelper.registerKeyBinding(new KeyBinding("remove_mode", GLFW.GLFW_KEY_LEFT_ALT, "landmark"));
		//ClientTickEvents.END_CLIENT_TICK.register((client) -> {
		//	boolean isPressed = DELETE_MODE.isPressed();
		//	if (wasPressed && !isPressed && client.player != null && client.player.getMainHandStack().getItem() instanceof DeedItem) {
		//    	ClientSidePacketRegistry.INSTANCE.sendToServer(LandmarkNetworking.TOGGLE_DELETE_MODE, new PacketByteBuf(Unpooled.buffer()));
		//	}
		//
		//	wasPressed = isPressed;
		//});
	}
}
