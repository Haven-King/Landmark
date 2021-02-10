package dev.hephaestus.landmark.mixin.client;

import dev.hephaestus.landmark.api.LandmarkCache;
import dev.hephaestus.landmark.impl.client.LandmarkHandler;
import dev.hephaestus.landmark.impl.util.LandmarkHandlerHolder;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin extends World implements LandmarkHandlerHolder {
    @Unique private LandmarkHandler handler;

    protected ClientWorldMixin(MutableWorldProperties properties, RegistryKey<World> registryRef, DimensionType dimensionType, Supplier<Profiler> profiler, boolean isClient, boolean debugWorld, long seed) {
        super(properties, registryRef, dimensionType, profiler, isClient, debugWorld, seed);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void initHandler(ClientPlayNetworkHandler networkHandler, ClientWorld.Properties properties, RegistryKey<World> registryRef, DimensionType dimensionType, int loadDistance, Supplier<Profiler> profiler, WorldRenderer worldRenderer, boolean debugWorld, long seed, CallbackInfo ci) {
        this.handler = new LandmarkHandler();
    }

    @Inject(method = "disconnect", at = @At("HEAD"))
    private void clearLandmarkCache(CallbackInfo ci) {
        LandmarkCache.getInstance(this).clear();
        this.handler.clear();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tickLandmarks(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        this.handler.tick(LandmarkCache.getInstance(this), this.getTime());
    }

    @Override
    public LandmarkHandler getLandmarkHandler() {
        return this.handler;
    }
}
