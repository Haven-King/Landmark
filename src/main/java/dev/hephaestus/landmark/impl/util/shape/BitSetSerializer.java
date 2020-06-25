package dev.hephaestus.landmark.impl.util.shape;

import dev.hephaestus.landmark.impl.util.Taggable;
import net.minecraft.nbt.CompoundTag;

import java.util.BitSet;

public class BitSetSerializer implements Taggable<BitSet> {
	public static final BitSetSerializer INSTANCE = new BitSetSerializer();

	@Override
	public CompoundTag toTag(CompoundTag tag, BitSet bitSet) {
		tag.putByteArray("bits", bitSet.toByteArray());
		return tag;
	}

	@Override
	public BitSet fromTag(CompoundTag tag) {
		return BitSet.valueOf(tag.getByteArray("bits"));
	}
}
