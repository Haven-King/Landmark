package dev.hephaestus.landmark.api;

import dev.hephaestus.landmark.impl.util.LandmarkLocationPredicate;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Identifier;
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

	public boolean test(ServerPlayerEntity player) {
		return this.predicate.test(player);
	}

	public boolean test(StructureStart<?> structureStart, BlockPos pos, ServerWorldAccess access) {
		return this.predicate.test(structureStart, pos, access);
	}
}
