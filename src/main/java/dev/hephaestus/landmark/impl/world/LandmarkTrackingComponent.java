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
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import io.netty.buffer.Unpooled;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
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

public class LandmarkTrackingComponent implements AutoSyncedComponent {
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
		}
	}

	public void add(Landmark landmark, BlockPos center) {
		if (!this.generatedLandmarks.containsKey(center)) {
			this.landmarks.put(landmark.getId(), landmark);
			this.generatedLandmarks.put(center, landmark);
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
	public void readFromNbt(CompoundTag tag) {
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
	public void writeToNbt(CompoundTag tag) {
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
	}

	public MutableText getName(UUID landmark) {
		Landmark l = this.landmarks.get(landmark);
		return l == null ? (MutableText) LiteralText.EMPTY : l.getName();
	}

	@Environment(EnvType.CLIENT)
	public static void read(MinecraftClient client, ClientPlayNetworkHandler network, PacketByteBuf buf, PacketSender sender) {
		CompoundTag tag = buf.readCompoundTag();

		client.execute(() -> {
			if (tag != null) {
				LandmarkClient.TRACKER.readFromNbt(tag);
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

	public World getWorld() {
		return this.world;
	}

	public static void delete(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler network, PacketByteBuf buf, PacketSender sender) {
		UUID id = buf.readUuid();
		boolean wasEditScreen = buf.readBoolean();
		Hand hand = null;

		if (wasEditScreen) {
			hand = buf.readEnumConstant(Hand.class);
		}

		Hand finalHand = hand;
		server.execute(() -> {
			LandmarkTrackingComponent tracker = of(player.getEntityWorld());
			Landmark landmark = tracker.get(id);

			if (landmark.canModify(player)) {
				for (ChunkPos pos : landmark.getChunks()) {
					LandmarkChunkComponent component = LandmarkMod.CHUNK_COMPONENT.get(landmark.getWorld().getChunk(pos.x, pos.z));
					component.remove(landmark);
				}

				tracker.landmarks.remove(id);

				if (finalHand != null) {
					player.setStackInHand(finalHand, new ItemStack(player.getStackInHand(finalHand).getItem()));
				}

			} else {
				player.sendMessage(new TranslatableText("deeds.landmark.delete.fail", new TranslatableText("deeds.landmark.fail.permissions")), true);
			}
		});
	}

	public static void claim(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler network, PacketByteBuf buf, PacketSender sender) {
		UUID id = buf.readUuid();
		Hand hand = buf.readEnumConstant(Hand.class);

		server.execute(() -> {
			LandmarkTrackingComponent tracker = of(player.getEntityWorld());
			Landmark landmark = tracker.get(id);

			if (landmark != null && landmark.canModify(player)) {
				landmark.withOwner(player);
				CompoundTag tag = player.getStackInHand(hand).getOrCreateTag();
				tag.putUuid("landmark_id", id);
				tag.putString("world_key", player.getEntityWorld().getRegistryKey().getValue().toString());
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

	public static void newLandmark(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler network, PacketByteBuf buf, PacketSender sender) {
		Hand hand = buf.readEnumConstant(Hand.class);
		BlockPos pos = buf.readBoolean() ? buf.readBlockPos() : null;

		server.execute(() -> {
			if (pos != null) {
				player.getStackInHand(hand).getOrCreateTag().putLong("marker", pos.asLong());
			}

			DeedItem.Data data = new DeedItem.Data(player.getEntityWorld(), player, player.getStackInHand(hand).getOrCreateTag());

			PacketByteBuf newBuf = new PacketByteBuf(Unpooled.buffer());
			newBuf.writeEnumConstant(hand);
			newBuf.writeUuid(data.landmarkId);
			sender.sendPacket(LandmarkNetworking.OPEN_EDIT_SCREEN, newBuf);
		});
	}
}
