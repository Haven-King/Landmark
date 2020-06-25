package dev.hephaestus.landmark.impl.util;

import net.minecraft.nbt.CompoundTag;

public interface Taggable<T> {
	CompoundTag toTag(CompoundTag tag, T object);
	T fromTag(CompoundTag tag);
}
