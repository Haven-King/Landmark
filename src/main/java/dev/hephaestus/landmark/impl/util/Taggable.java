package dev.hephaestus.landmark.impl.util;

import net.minecraft.nbt.CompoundTag;

//TODO: codec?
public interface Taggable<T> {
	CompoundTag toTag(CompoundTag tag, T object);
	T fromTag(CompoundTag tag);
}
