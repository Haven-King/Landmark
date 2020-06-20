package dev.hephaestus.landmark.impl.util.shape;

import net.minecraft.nbt.CompoundTag;

public interface Serializer<T> {
    CompoundTag toTag(T object, CompoundTag tag);
    T fromTag(CompoundTag tag);
}
