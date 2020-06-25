package dev.hephaestus.landmark.impl.landmarks;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.UUID;

import dev.hephaestus.landmark.impl.item.DeedItem;
import dev.hephaestus.landmark.impl.world.LandmarkTrackingComponent;
import io.netty.util.internal.ConcurrentSet;

import net.minecraft.client.util.math.Vector3f;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import net.fabricmc.fabric.api.network.PacketContext;

public abstract class Landmark {
	protected final Collection<ChunkPos> chunks = new ConcurrentSet<>();
	private HashSet<UUID> owners = new HashSet<>();

	private World world;
	private UUID id;
	private MutableText name;
	private Vector3f color;

	protected Landmark(World world, UUID id, MutableText name) {
		this.world = world;
		this.id = id;
		this.setName(name);
	}

	public Landmark withOwner(PlayerEntity playerEntity) {
		this.owners.add(playerEntity.getUuid());
		return this;
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
		return this.owners.isEmpty() || this.owners.contains(playerEntity.getUuid()) || playerEntity.hasPermissionLevel(2);
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

		ListTag owners = tag.getList("owners", 8);

		for (UUID owner : this.owners) {
			owners.add(StringTag.of(owner.toString()));
		}

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

		this.owners = new HashSet<>();
		ListTag owners = tag.getList("owners", 8);

		for (Tag owner : owners) {
			this.owners.add(UUID.fromString(owner.asString()));
		}

		return this;
	}

	public static Landmark from(World world, CompoundTag tag) {
		String type = tag.getString("type");
		UUID id = tag.getUuid("id");
		MutableText name = Text.Serializer.fromJson(tag.getString("name"));

		if ("player".equals(type)) {
			return new PlayerLandmark(world, name).fromTag(world, tag).with(id);
		} else {
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

	public static void saveName(PacketContext context, PacketByteBuf buf) {
		UUID id = buf.readUuid();
		MutableText name = (MutableText) buf.readText();
		Hand hand = buf.readEnumConstant(Hand.class);

		context.getTaskQueue().execute(() -> {
			ItemStack stack = context.getPlayer().getStackInHand(hand);

			if (stack.getItem() instanceof DeedItem) {
				CompoundTag tag = stack.getOrCreateTag();
				ServerWorld world = (ServerWorld) context.getPlayer().getEntityWorld();
				DeedItem.Data data = new DeedItem.Data(world, context.getPlayer(), tag);

				LandmarkTrackingComponent tracker = LandmarkTrackingComponent.of(world);
				Landmark landmark = tracker.get(id);

				if (data.landmarkId.equals(id)) {
					if (landmark.canModify(context.getPlayer())) {
						tag.putString("landmark_name", Text.Serializer.toJson(name));
						landmark.setName(name);
						tracker.sync();
					} else {
						context.getPlayer().sendMessage(new TranslatableText("deeds.landmark.rename.fail"), true);
					}
				}
			}
		});
	}
}
