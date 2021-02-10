package dev.hephaestus.landmark.impl.util;

import net.minecraft.client.render.BufferBuilderStorage;

@FunctionalInterface
public interface BufferBuilderSupplier {
    BufferBuilderStorage getBufferBuilders();
}
