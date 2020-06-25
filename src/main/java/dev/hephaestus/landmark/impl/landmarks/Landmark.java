package dev.hephaestus.landmark.impl.landmarks;

import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

import dev.hephaestus.landmark.impl.world.LandmarkTrackingComponent;
import io.netty.util.internal.ConcurrentSet;

import net.minecraft.client.util.math.Vector3f;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

public abstract class Landmark {
	protected final Collection<ChunkPos> chunks = new ConcurrentSet<>();

	private World world;
	private UUID id;
	private MutableText name;
	private Vector3f color;

	protected Landmark(World world, UUID id, MutableText name) {
		this.world = world;
		this.id = id;
		this.setName(name);
	}

	public final void setName(MutableText name) {
		this.name = name;
		TextColor color = this.name.getStyle().getColor();

		if (color != null) {
			int i = color.rgb;
			this.color = new Vector3f(
					((float) ((i >>> 16) & 0xFF)) / 255F,
					((float) ((i >>> 8) & 0xFF)) / 255F,
					((float) (i & 0xFF)) / 255F
			);
		} else {
			this.color = new Vector3f(1F, 1F, 1F);
		}

		LandmarkTrackingComponent.of(this.world).sync();
	}

	public abstract boolean add(LandmarkSection section);

	public final World getWorld() {
		return this.world;
	}

	public final UUID getId() {
		return this.id;
	}

	public final MutableText getName() {
		return this.name;
	}

	public final Vector3f getColor() {
		return this.color;
	}

	public boolean canModify(PlayerEntity playerEntity) {
		return playerEntity.hasPermissionLevel(2);
	}

	protected final Landmark with(UUID id) {
		this.id = id;
		return this;
	}

	public CompoundTag toTag(CompoundTag tag) {
		tag.putUuid("id", this.id);
		tag.putString("name", Text.Serializer.toJson(this.name));

		ListTag chunks = tag.getList("chunks", 4);

		for (ChunkPos pos : this.chunks) {
			chunks.add(LongTag.of(pos.toLong()));
		}

		tag.put("chunks", chunks);

		return tag;
	}

	public Landmark fromTag(World world, CompoundTag tag) {
		this.world = world;
		this.id = tag.getUuid("id");
		this.setName(Text.Serializer.fromJson(tag.getString("name")));

		ListTag chunks = tag.getList("chunks", 4);

		for (Tag chunk : chunks) {
			this.chunks.add(new ChunkPos(((LongTag) chunk).getLong()));
		}

		return this;
	}

	public static Landmark from(World world, CompoundTag tag) {
		String type = tag.getString("type");
		UUID id = tag.getUuid("id");
		MutableText name = Text.Serializer.fromJson(tag.getString("name"));
		switch (type) {
		case "player":
			return new PlayerLandmark(world, name).fromTag(world, tag).with(id);
		default:
			return new GeneratedLandmark(world, BlockPos.fromLong(tag.getLong("center")), name).with(id);
		}
	}

	public final Iterable<? extends ChunkPos> getChunks() {
		return this.chunks;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Landmark landmark = (Landmark) o;
		return Objects.equals(id, landmark.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
