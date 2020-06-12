package dev.hephaestus.landmark.api;

import net.minecraft.predicate.entity.LocationPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.StructureFeature;

public class LandmarkType {
	private final Identifier id;
	private final Identifier name_generator;
	private final LocationPredicate predicate;

	public LandmarkType(Identifier id, Identifier name_generator, LocationPredicate predicate) {
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
		return predicate.feature;
	}

	public boolean test(ServerWorld world, double x, double y, double z) {
		return this.predicate.test(world, x, y, z);
	}

	public boolean test(ServerPlayerEntity player) {
		return test(player.getServerWorld(), player.getX(), player.getY(), player.getZ());
	}
}
