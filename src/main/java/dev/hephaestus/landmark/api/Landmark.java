package dev.hephaestus.landmark.api;

import net.minecraft.predicate.entity.LocationPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

public class Landmark {
	private final Identifier id;
	private final LocationPredicate predicate;

	public Landmark(Identifier id, LocationPredicate predicate) {
		this.id = id;
		this.predicate = predicate;
	}

	public Identifier getId() {
		return this.id;
	}

	public boolean test(ServerWorld world, double x, double y, double z) {
		return this.predicate.test(world, x, y, z);
	}

	public boolean test(ServerPlayerEntity player) {
		return test(player.getServerWorld(), player.getX(), player.getY(), player.getZ());
	}
}
