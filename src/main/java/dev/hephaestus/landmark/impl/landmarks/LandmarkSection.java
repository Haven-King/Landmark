package dev.hephaestus.landmark.impl.landmarks;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.UUID;

import dev.hephaestus.landmark.impl.world.LandmarkTrackingComponent;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.ChunkPos;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public class LandmarkSection implements Comparable<LandmarkSection> {
	public final UUID parent;
	public final double minX;
	public final double minY;
	public final double minZ;
	public final double maxX;
	public final double maxY;
	public final double maxZ;
	public final double volume;
	private final Collection<ChunkPos> chunks;

	public LandmarkSection(UUID parent, BlockBox boundingBox) {
		this(
				parent,
				boundingBox.minX,
				boundingBox.minY,
				boundingBox.minZ,
				boundingBox.maxX + 1,
				boundingBox.maxY + 1,
				boundingBox.maxZ + 1
		);
	}

	public LandmarkSection(UUID parent, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		this.parent = parent;
		this.minX = minX;
		this.minY = minY;
		this.minZ = minZ;
		this.maxX = maxX;
		this.maxY = maxY;
		this.maxZ = maxZ;
		this.volume = (this.maxX - this.minX) * (this.maxY - this.minY) * (this.maxZ - this.minZ);
		chunks = new HashSet<>();

		for (int x = (int) minX >> 4; x <= (int) maxX >> 4; ++x) {
			for (int z = (int) minZ >> 4; z <= (int) maxZ >> 4; ++z) {
				this.chunks.add(new ChunkPos(x, z));
			}
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		LandmarkSection section = (LandmarkSection) o;
		return Double.compare(section.minX, minX) == 0
				&& Double.compare(section.minY, minY) == 0
				&& Double.compare(section.minZ, minZ) == 0
				&& Double.compare(section.maxX, maxX) == 0
				&& Double.compare(section.maxY, maxY) == 0
				&& Double.compare(section.maxZ, maxZ) == 0
				&& parent.equals(section.parent);
	}

	@Override
	public int hashCode() {
		return Objects.hash(parent, minX, minY, minZ, maxX, maxY, maxZ);
	}

	public boolean contains(double x, double y, double z) {
		return x >= this.minX && x < this.maxX && y >= this.minY && y < this.maxY && z >= this.minZ && z < this.maxZ;
	}

	public boolean matches(UUID parent) {
		return parent.equals(this.parent);
	}

	public Collection<ChunkPos> getChunks() {
		return chunks;
	}

	@Override
	public int compareTo(LandmarkSection section) {
		// This is backwards because we want them inserted into the queue in reverse order.
		return Double.compare(section == null ? 0D : section.volume, this.volume);
	}

	public CompoundTag toTag(CompoundTag tag) {
		tag.putUuid("parent", this.parent);
		tag.putDouble("minX", this.minX);
		tag.putDouble("minY", this.minY);
		tag.putDouble("minZ", this.minZ);
		tag.putDouble("maxX", this.maxX);
		tag.putDouble("maxY", this.maxY);
		tag.putDouble("maxZ", this.maxZ);

		ListTag chunksTag = tag.getList("chunks", 4);

		for (ChunkPos pos : this.chunks) {
			chunksTag.add(LongTag.of(pos.toLong()));
		}

		return tag;
	}

	public static LandmarkSection fromTag(CompoundTag tag) {
		return new LandmarkSection(
				tag.getUuid("parent"),
				tag.getDouble("minX"),
				tag.getDouble("minY"),
				tag.getDouble("minZ"),
				tag.getDouble("maxX"),
				tag.getDouble("maxY"),
				tag.getDouble("maxZ")
		);
	}

	@Environment(EnvType.CLIENT)
	public void render(MatrixStack matrices, VertexConsumer vertexConsumer) {
		float alpha = 0.25F;

		PlayerEntity player = MinecraftClient.getInstance().player;

		if (player != null) {
			double dx = ((this.minX + this.maxX) / 2D) - player.getX();
			double dy = ((this.minY + this.maxY) / 2D) - player.getY();
			double dz = ((this.minZ + this.maxZ) / 2D) - player.getZ();

			if (dx < 300 && dy < 300 && dz < 300) {
				CompoundTag tag = player.getMainHandStack().getOrCreateTag();

				if (tag.contains("landmark_id")) {
					UUID id = tag.getUuid("landmark_id");
					alpha = id.equals(this.parent) ? 1F : 0.25F;
				}

				Landmark landmark = LandmarkTrackingComponent.of(MinecraftClient.getInstance().world).get(this.parent);

				if (landmark != null) {
					Vector3f color = landmark.getColor();
					WorldRenderer.drawBox(matrices, vertexConsumer, this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ, color.getX(), color.getY(), color.getZ(), alpha);
				}
			}
		}
	}
}
