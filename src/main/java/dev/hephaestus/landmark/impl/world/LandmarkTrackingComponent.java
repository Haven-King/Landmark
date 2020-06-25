package dev.hephaestus.landmark.impl.world;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.ConcurrentHashMultiset;
import dev.hephaestus.landmark.impl.LandmarkClient;
import dev.hephaestus.landmark.impl.LandmarkMod;
import dev.hephaestus.landmark.impl.item.DeedItem;
import dev.hephaestus.landmark.impl.landmarks.GeneratedLandmark;
import dev.hephaestus.landmark.impl.landmarks.Landmark;
import dev.hephaestus.landmark.impl.landmarks.PlayerLandmark;
import dev.hephaestus.landmark.impl.network.LandmarkNetworking;
import dev.hephaestus.landmark.impl.world.chunk.LandmarkChunkComponent;
import io.netty.buffer.Unpooled;
import nerdhub.cardinal.components.api.util.sync.WorldSyncedComponent;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.network.PacketContext;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;

public class LandmarkTrackingComponent implements WorldSyncedComponent {
	private final World world;

	private final ConcurrentHashMap<BlockPos, Landmark> generatedLandmarks = new ConcurrentHashMap<>();

	private ConcurrentHashMap<UUID, Landmark> landmarks = new ConcurrentHashMap<>();
	private ConcurrentHashMap<ChunkPos, ConcurrentHashMultiset<Landmark>> searcher = new ConcurrentHashMap<>();

	public LandmarkTrackingComponent(World world) {
		this.world = world;
	}

	public static void add(ServerWorld world, Landmark landmark) {
		of(world).add(landmark);
	}

	public void add(Landmark landmark) {
		if (landmark instanceof GeneratedLandmark) {
			this.add(landmark, ((GeneratedLandmark) landmark).getCenter());
		} else {
			this.landmarks.put(landmark.getId(), landmark);
			this.sync();
		}
	}

	public void add(Landmark landmark, BlockPos center) {
		if (!this.generatedLandmarks.containsKey(center)) {
			this.landmarks.put(landmark.getId(), landmark);
			this.generatedLandmarks.put(center, landmark);
			this.sync();
		}
	}

	public static LandmarkTrackingComponent of(World world) {
		return LandmarkMod.TRACKING_COMPONENT.get(world);
	}

	public Landmark get(UUID uuid) {
		return this.landmarks.get(uuid);
	}

	public Landmark get(BlockPos pos) {
		return this.generatedLandmarks.get(pos);
	}

	@Override
	public void fromTag(CompoundTag tag) {
		this.landmarks = new ConcurrentHashMap<>();
		this.searcher = new ConcurrentHashMap<>();

		CompoundTag landmarksTag = tag.getCompound("landmarks");

		for (String key : landmarksTag.getKeys()) {
			Landmark landmark = Landmark.from(this.world, landmarksTag.getCompound(key));
			this.landmarks.put(UUID.fromString(key), landmark);

			for (ChunkPos pos : landmark.getChunks()) {
				this.searcher.computeIfAbsent(pos, (p) -> ConcurrentHashMultiset.create()).add(landmark);
			}
		}

		CompoundTag generatedTag = tag.getCompound("generated");

		for (String key : generatedTag.getKeys()) {
			this.generatedLandmarks.put(
					BlockPos.fromLong(Long.parseLong(key)),
					Landmark.from(this.world, generatedTag.getCompound(key))
			);
		}
	}

	@Override
	public CompoundTag toTag(CompoundTag tag) {
		CompoundTag landmarksTag = new CompoundTag();

		for (Landmark landmark : this.landmarks.values()) {
			landmarksTag.put(landmark.getId().toString(), landmark.toTag(new CompoundTag()));
		}

		tag.put("landmarks", landmarksTag);

		CompoundTag generatedTag = new CompoundTag();

		for (Map.Entry<BlockPos, Landmark> entry : this.generatedLandmarks.entrySet()) {
			generatedTag.put(String.valueOf(entry.getKey().asLong()), entry.getValue().toTag(new CompoundTag()));
		}

		tag.put("generated", generatedTag);

		return tag;
	}

	public MutableText getName(UUID landmark) {
		Landmark l = this.landmarks.get(landmark);
		return l == null ? (MutableText) LiteralText.EMPTY : l.getName();
	}

	@Environment(EnvType.CLIENT)
	public static void read(PacketContext context, PacketByteBuf buf) {
		CompoundTag tag = buf.readCompoundTag();

		context.getTaskQueue().execute(() -> {
			if (tag != null) {
				LandmarkClient.TRACKER.fromTag(tag);
			}
		});
	}

	public Collection<Landmark> get(ChunkPos pos) {
		return this.searcher.get(pos);
	}

	public void put(ChunkPos pos, PlayerLandmark landmark) {
		this.searcher.computeIfAbsent(pos, (p) -> ConcurrentHashMultiset.create()).add(landmark);
	}

	public boolean contains(BlockPos pos) {
		return this.generatedLandmarks.containsKey(pos);
	}

	@Override
	public World getWorld() {
		return this.world;
	}

	public static void delete(PacketContext context, PacketByteBuf buf) {
		UUID id = buf.readUuid();
		boolean wasEditScreen = buf.readBoolean();
		Hand hand = null;

		if (wasEditScreen) {
			hand = buf.readEnumConstant(Hand.class);
		}

		Hand finalHand = hand;
		context.getTaskQueue().execute(() -> {
			LandmarkTrackingComponent tracker = of(context.getPlayer().getEntityWorld());
			Landmark landmark = tracker.get(id);

			if (landmark.canModify(context.getPlayer())) {
				for (ChunkPos pos : landmark.getChunks()) {
					LandmarkChunkComponent component = LandmarkMod.CHUNK_COMPONENT.get(landmark.getWorld().getChunk(pos.x, pos.z));
					component.remove(landmark);
					component.sync();
				}

				tracker.landmarks.remove(id);

				if (finalHand != null) {
					context.getPlayer().setStackInHand(finalHand, new ItemStack(context.getPlayer().getStackInHand(finalHand).getItem()));
				}

				tracker.sync();
			} else {
				context.getPlayer().sendMessage(new TranslatableText("deeds.landmark.delete.fail", new TranslatableText("deeds.landmark.fail.permissions")), true);
			}
		});
	}

	public static void claim(PacketContext context, PacketByteBuf packetByteBuf) {
		UUID id = packetByteBuf.readUuid();
		Hand hand = packetByteBuf.readEnumConstant(Hand.class);

		context.getTaskQueue().execute(() -> {
			LandmarkTrackingComponent tracker = of(context.getPlayer().getEntityWorld());
			Landmark landmark = tracker.get(id);

			if (landmark != null && landmark.canModify(context.getPlayer())) {
				landmark.withOwner(context.getPlayer());
				CompoundTag tag = context.getPlayer().getStackInHand(hand).getOrCreateTag();
				tag.putUuid("landmark_id", id);
				tag.putString("world_key", context.getPlayer().getEntityWorld().getRegistryKey().getValue().toString());
				tag.putString("landmark_name", Text.Serializer.toJson(landmark.getName()));

				if (landmark instanceof GeneratedLandmark) {
					tag.putBoolean("is_generated", true);
				}

				if (landmark instanceof PlayerLandmark) {
					tag.putDouble("volume", ((PlayerLandmark) landmark).volume());
				}
			}
		});
	}

	public static void newLandmark(PacketContext context, PacketByteBuf packetByteBuf) {
		Hand hand = packetByteBuf.readEnumConstant(Hand.class);

		context.getTaskQueue().execute(() -> {
			DeedItem.Data data = new DeedItem.Data(context.getPlayer().getEntityWorld(), context.getPlayer(), context.getPlayer().getStackInHand(hand).getOrCreateTag());

			PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
			buf.writeEnumConstant(hand);
			buf.writeUuid(data.landmarkId);
			ServerSidePacketRegistry.INSTANCE.sendToPlayer(context.getPlayer(), LandmarkNetworking.OPEN_EDIT_SCREEN, buf);
		});
	}
}
