package dev.hephaestus.landmark.api;

import dev.hephaestus.landmark.impl.util.LandmarkLocationPredicate;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.gen.feature.StructureFeature;

public class LandmarkType {
	private final Identifier id;
	private final Identifier name_generator;
	private final LandmarkLocationPredicate predicate;

	public LandmarkType(Identifier id, Identifier name_generator, LandmarkLocationPredicate predicate) {
		this.id = id;
		this.name_generator = name_generator;
		this.predicate = predicate;
	}

	public Identifier getId() {
		return this.id;
	}

	public Identifier getNameGeneratorId() {
		return this.name_generator;
	}

	public StructureFeature<?> getFeature() {
		return predicate.getFeature();
	}

	public Pair<Integer, LandmarkType> test(ServerPlayerEntity player) {
		return new Pair<>(this.predicate.test(player), this);
	}

	public Pair<Integer, LandmarkType> test(StructureStart<?> structureStart, BlockPos pos, ServerWorldAccess access) {
		return new Pair<>(this.predicate.test(structureStart, pos, access), this);
	}
}
