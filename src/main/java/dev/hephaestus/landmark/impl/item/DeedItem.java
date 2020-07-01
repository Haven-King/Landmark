package dev.hephaestus.landmark.impl.item;

import java.util.List;
import java.util.UUID;

import dev.hephaestus.landmark.impl.landmarks.Landmark;
import dev.hephaestus.landmark.impl.landmarks.LandmarkSection;
import dev.hephaestus.landmark.impl.landmarks.PlayerLandmark;
import dev.hephaestus.landmark.impl.network.LandmarkNetworking;
import dev.hephaestus.landmark.impl.world.LandmarkTrackingComponent;
import io.netty.buffer.Unpooled;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.RayTraceContext;
import net.minecraft.world.World;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.fabricmc.fabric.api.network.PacketContext;

public class DeedItem extends Item {
	private final double maxVolume;

	public DeedItem(Settings settings, double maxVolume) {
		super(settings);
		this.maxVolume = maxVolume;
	}

	@Override
	public boolean hasGlint(ItemStack stack) {
		return stack.getRarity() == Rarity.EPIC;
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		if (!context.getWorld().isClient && context.getPlayer() != null) {
			return this.useOnBlock((ServerPlayerEntity) context.getPlayer(), context.getHand(), context.getBlockPos());
		}

		return ActionResult.SUCCESS;
	}

	private ActionResult useOnBlock(ServerPlayerEntity playerEntity, Hand hand, BlockPos marker) {
		ItemStack stack = validatedDeed(playerEntity.getServerWorld(), playerEntity.getStackInHand(hand));

		if (stack.hasTag()) {
			CompoundTag tag = stack.getOrCreateTag();
			Data data = new Data(playerEntity.getServerWorld(), playerEntity, tag);

			if (data.isGenerated) {
				return ActionResult.PASS;
			}

			ServerWorld world = playerEntity.getServerWorld();

			if (data.marker != null) {
				if (!data.world.equals(world.getRegistryKey())) {
					playerEntity.sendMessage(new TranslatableText("deeds.landmark.fail.world"), true);
					return ActionResult.FAIL;
				}

				tag.remove("marker");

				BlockBox newBox = new BlockBox(data.marker, marker);

				LandmarkTrackingComponent trackingComponent = LandmarkTrackingComponent.of(world);
				PlayerLandmark landmark = (PlayerLandmark) trackingComponent.get(data.landmarkId);

				if (landmark.canModify(playerEntity)) {
					BooleanBiFunction function = /*data.deleteMode ? BooleanBiFunction.NOT_SECOND :*/ BooleanBiFunction.OR;
					int result = landmark.add(new LandmarkSection(landmark.getId(), newBox), this.maxVolume, function, true);

					if (result == 0) {
						double volume = landmark.volume();
						landmark.makeSections();
						tag.putDouble("volume", volume);
						playerEntity.sendMessage(new TranslatableText("deeds.landmark.success.add_box", volume), true);
						return ActionResult.SUCCESS;
					} else if (result == 1) {
						playerEntity.sendMessage(new TranslatableText("deeds.landmark.fail.overlap", this.maxVolume), true);
						return ActionResult.FAIL;
					} else if (result == 2) {
						playerEntity.sendMessage(new TranslatableText("deeds.landmark.fail.volume", this.maxVolume), true);
						return ActionResult.FAIL;
					} else {
						playerEntity.sendMessage(new TranslatableText("deeds.landmark.fail.other", this.maxVolume), true);
						return ActionResult.FAIL;
					}
				} else {
					playerEntity.sendMessage(new TranslatableText("deeds.landmark.fail.permissions"), true);
					return ActionResult.FAIL;
				}
			} else {
				tag.putLong("marker", marker.asLong());
			}
		} else {
			PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
			buf.writeEnumConstant(hand);
			buf.writeBoolean(true);
			buf.writeBlockPos(marker);
			ServerSidePacketRegistry.INSTANCE.sendToPlayer(playerEntity, LandmarkNetworking.OPEN_CLAIM_SCREEN, buf);
		}

		return ActionResult.SUCCESS;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
		CompoundTag tag = stack.getTag();
		int volume = 0;

		if (tag != null && tag.contains("volume", 6)) {
			volume = (int) tag.getDouble("volume");
		}

		if (tag != null && (!tag.contains("is_generated") || !tag.getBoolean("is_generated"))) {
			if (this.maxVolume < Double.MAX_VALUE) {
				tooltip.add(new TranslatableText("item.landmark.deed.volume", volume, (int) this.maxVolume).styled((style) -> style.withItalic(true).withColor(Formatting.DARK_GRAY)));
			} else {
				tooltip.add(new TranslatableText("item.landmark.deed.volume.creative", volume).styled((style) -> style.withItalic(true).withColor(Formatting.DARK_GRAY)));
			}
		}

		if (tag != null && tag.contains("landmark_name")) {
			tooltip.add(Text.Serializer.fromJson(tag.getString("landmark_name")));
		}

		if (tag != null && tag.contains("landmark_id") && FabricLoader.getInstance().isDevelopmentEnvironment()) {
			tooltip.add(new LiteralText(tag.getUuid("landmark_id").toString()));
		}

		super.appendTooltip(stack, world, tooltip, context);
	}

	private static BlockHitResult traceForBlock(ServerPlayerEntity player) {
		double d = 5;
		Vec3d angle = player.getRotationVec(1F);
		return player.world.rayTrace(new RayTraceContext(
				player.getCameraPosVec(1F),
				player.getCameraPosVec(1F).add(d * angle.x, d * angle.y, d * angle.z),
				RayTraceContext.ShapeType.OUTLINE, RayTraceContext.FluidHandling.NONE, player
		));
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		if (!world.isClient) {
			if (user.getStackInHand(hand).hasTag()) {
				if (user.isSneaking()) {
					this.useOnBlock((ServerPlayerEntity) user, hand, traceForBlock((ServerPlayerEntity) user).getBlockPos());
				} else {
					CompoundTag tag = validatedDeed((ServerWorld) world, user.getStackInHand(hand)).getOrCreateTag();
					Data data = new Data(world, user, tag);

					PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
					buf.writeEnumConstant(hand);
					buf.writeUuid(data.landmarkId);
					ServerSidePacketRegistry.INSTANCE.sendToPlayer(user, LandmarkNetworking.OPEN_EDIT_SCREEN, buf);
				}
			} else {
				PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
				buf.writeEnumConstant(hand);
				buf.writeBoolean(false);
				ServerSidePacketRegistry.INSTANCE.sendToPlayer(user, LandmarkNetworking.OPEN_CLAIM_SCREEN, buf);
			}
		}

		return super.use(world, user, hand);
	}

	private static ItemStack validatedDeed(ServerWorld world, ItemStack deedItem) {
		if (deedItem.getItem() instanceof DeedItem && deedItem.hasTag()) {
			CompoundTag tag = deedItem.getOrCreateTag();

			if (tag.contains("landmark_id")) {
				UUID landmarkId = tag.getUuid("landmark_id");

				if (LandmarkTrackingComponent.of(world).get(landmarkId) == null) {
					deedItem.setTag(new CompoundTag());
				}
			}
		}

		return deedItem;
	}

	public static void toggleDeleteMode(PacketContext context, PacketByteBuf packetByteBuf) {
		context.getTaskQueue().execute(() -> {
			ItemStack stack = context.getPlayer().getMainHandStack();

			if (stack.getItem() instanceof DeedItem) {
				CompoundTag tag = context.getPlayer().getMainHandStack().getOrCreateTag();

				if (tag.contains("delete_mode")) {
					tag.putBoolean("delete_mode", !tag.getBoolean("delete_mode"));
				} else {
					tag.putBoolean("delete_mode", true);
				}
			}
		});
	}

	public static class Data {
		public final UUID landmarkId;
		public final RegistryKey<World> world;
		public final MutableText landmarkName;
		public final boolean isGenerated;
		public final double volume;
		public final BlockPos marker;
		public final boolean deleteMode;

		public Data(World world, PlayerEntity player, CompoundTag tag) {
			if (tag.contains("landmark_id")) {
				this.landmarkId = tag.getUuid("landmark_id");
				RegistryKey<World> tagWorld = RegistryKey.of(Registry.DIMENSION, new Identifier(tag.getString("world_key")));

				if (world.getServer() != null && LandmarkTrackingComponent.of(world.getServer().getWorld(tagWorld)).get(this.landmarkId) != null) {
					this.world = tagWorld;
					this.landmarkName = world.getServer() != null ? LandmarkTrackingComponent.of(world.getServer().getWorld(this.world)).get(this.landmarkId).getName() : null;
					this.isGenerated = tag.contains("is_generated") && tag.getBoolean("is_generated");
					this.volume = tag.contains("volume") ? tag.getDouble("volume") : 0;
					this.marker = tag.contains("marker") ? BlockPos.fromLong(tag.getLong("marker")) : null;
					this.deleteMode = tag.contains("delete_mode") && tag.getBoolean("delete_mode");
				} else {
					Landmark landmark = new PlayerLandmark(world, this.landmarkId);

					if (player != null) {
						landmark = landmark.withOwner(player);
					}

					LandmarkTrackingComponent.add((ServerWorld) world, landmark);
					tag.putUuid("landmark_id", this.landmarkId);

					tag.putString("world_key", world.getRegistryKey().getValue().toString());
					this.world = world.getRegistryKey();

					this.landmarkName = landmark.getName();
					tag.putString("landmark_name", Text.Serializer.toJson(this.landmarkName));

					tag.putBoolean("is_generated", false);
					this.isGenerated = false;

					tag.putDouble("volume", 0);
					this.volume = 0;

					this.marker = null;

					this.deleteMode = false;
				}
			} else {
				Landmark landmark = new PlayerLandmark(world);

				if (player != null) {
					landmark = landmark.withOwner(player);
				}

				LandmarkTrackingComponent.add((ServerWorld) world, landmark);
				tag.putUuid("landmark_id", landmark.getId());
				this.landmarkId = landmark.getId();

				tag.putString("world_key", world.getRegistryKey().getValue().toString());
				this.world = world.getRegistryKey();

				this.landmarkName = landmark.getName();
				tag.putString("landmark_name", Text.Serializer.toJson(this.landmarkName));

				tag.putBoolean("is_generated", false);
				this.isGenerated = false;

				tag.putDouble("volume", 0);
				this.volume = 0;

				this.marker = null;

				this.deleteMode = false;
			}
		}

		public Data(World world, CompoundTag tag) {
			this(world, null, tag);
		}
	}
}
