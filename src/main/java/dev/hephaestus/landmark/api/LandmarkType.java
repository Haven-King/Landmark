package dev.hephaestus.landmark.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
	private final List<Identifier> name_generators = new ArrayList<>();
	private final LandmarkLocationPredicate predicate;
	private final TextColor color;

	public LandmarkType(Identifier id, LandmarkLocationPredicate predicate, TextColor color) {
		this.id = id;
		this.predicate = predicate;
		this.color = color;
	}

	public void addNameGenerator(Identifier id) {
		this.name_generators.add(id);
	}

	public Identifier getId() {
		return this.id;
	}

	public MutableText generateName() {
		return NameGenerator.generate(this.name_generators.get(new Random().nextInt(this.name_generators.size()))).styled(style -> style.withColor(this.color));
	}

	public StructureFeature<?> getFeature() {
		return predicate.getFeature();
	}

	public Pair<Integer, LandmarkType> test(StructureStart<?> structureStart, BlockPos pos, ServerWorldAccess access) {
		return new Pair<>(this.predicate.test(structureStart, pos, access), this);
	}
}
