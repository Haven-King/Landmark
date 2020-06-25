package dev.hephaestus.landmark.api;

import dev.hephaestus.landmark.impl.names.NameGenerator;
import dev.hephaestus.landmark.impl.util.LandmarkLocationPredicate;

import net.minecraft.structure.StructureStart;
import net.minecraft.text.MutableText;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.gen.feature.StructureFeature;

public class LandmarkType {
	private final Identifier id;
	private final Identifier name_generator;
	private final LandmarkLocationPredicate predicate;
	private final TextColor color;

	public LandmarkType(Identifier id, Identifier name_generator, LandmarkLocationPredicate predicate, TextColor color) {
		this.id = id;
		this.name_generator = name_generator;
		this.predicate = predicate;
		this.color = color;
	}

	public Identifier getId() {
		return this.id;
	}

	public MutableText generateName() {
		return NameGenerator.generate(this.name_generator).styled(style -> style.withColor(this.color));
	}

	public StructureFeature<?> getFeature() {
		return predicate.getFeature();
	}

	public Pair<Integer, LandmarkType> test(StructureStart<?> structureStart, BlockPos pos, ServerWorldAccess access) {
		return new Pair<>(this.predicate.test(structureStart, pos, access), this);
	}
}
