package dev.hephaestus.landmark.impl.mixin.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.hephaestus.landmark.impl.client.LandmarkNameHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public class GameRendererMixin {
	@Final
	@Shadow private MinecraftClient client;

	@Inject(method = "render", at = @At("TAIL"))
	private void renderNames(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
		if (!this.client.skipGameRender && tick && this.client.world != null) {
			RenderSystem.defaultAlphaFunc();
			LandmarkNameHandler.draw(new MatrixStack());
			RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);
		}
	}
}
