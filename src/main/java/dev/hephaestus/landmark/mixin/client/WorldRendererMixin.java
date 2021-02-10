package dev.hephaestus.landmark.mixin.client;

import dev.hephaestus.landmark.impl.util.BufferBuilderSupplier;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin implements BufferBuilderSupplier {
    @Shadow @Final private BufferBuilderStorage bufferBuilders;

    @Override
    public BufferBuilderStorage getBufferBuilders() {
        return this.bufferBuilders;
    }
}
