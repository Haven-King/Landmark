package dev.hephaestus.landmark.impl.item;

import java.util.List;
import java.util.UUID;

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
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;

public class DeedItem extends Item {

	private final double maxVolume;

	public DeedItem(Settings settings, double maxVolume) {
		super(settings);
		this.maxVolume = maxVolume;
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		if (context.getPlayer() == null) {
			return ActionResult.PASS;
		}

		ItemStack stack = context.getPlayer().getStackInHand(context.getHand());

		CompoundTag tag = stack.getTag();
		if (tag != null && tag.contains("is_generated") && tag.getBoolean("is_generated")) {
			return ActionResult.PASS;
		}

		if (!context.getWorld().isClient && context.getPlayer() != null) {
			if (stack.hasTag()) {
				verifyTag(context.getPlayer(), context.getHand());

				ServerWorld world = (ServerWorld) context.getWorld();


				tag = stack.getTag();

				if (tag.contains("marker")) {
					if (RegistryKey.of(Registry.DIMENSION, new Identifier(tag.getString("world_key"))) != world.getRegistryKey()) {
						context.getPlayer().sendMessage(new TranslatableText("deeds.landmark.fail.world"), true);
						return ActionResult.FAIL;
					}

					BlockPos marker1 = BlockPos.fromLong(tag.getLong("marker"));
					tag.remove("marker");
					BlockPos marker2 = context.getBlockPos();

					UUID id = tag.getUuid("landmark_id");

					BlockBox newBox = new BlockBox(marker1, marker2);

					LandmarkTrackingComponent trackingComponent = LandmarkTrackingComponent.of(world);
					PlayerLandmark landmark = (PlayerLandmark) trackingComponent.get(id);

					if (landmark.canModify(context.getPlayer())) {
						if (landmark.add(new LandmarkSection(landmark.getId(), newBox), this.maxVolume)) {
							double volume = landmark.volume();
							landmark.makeSections();
							tag.putDouble("volume", volume);
							context.getPlayer().sendMessage(new TranslatableText("deeds.landmark.success.add_box", volume), true);
							trackingComponent.sync();
							return ActionResult.SUCCESS;
						} else {
							context.getPlayer().sendMessage(new TranslatableText("deeds.landmark.fail.volume", this.maxVolume), true);
							return ActionResult.FAIL;
						}
					} else {
						context.getPlayer().sendMessage(new TranslatableText("deeds.landmark.fail.permissions"), true);
						return ActionResult.FAIL;
					}
				} else {
					if (!tag.contains("landmark_id")) {
						PlayerLandmark landmark = new PlayerLandmark(world);
						LandmarkTrackingComponent.add(world, landmark.withOwner(context.getPlayer()));
						tag.putUuid("landmark_id", landmark.getId());
					}

					if (!tag.contains("world_key")) {
						tag.putString("world_key", world.getRegistryKey().getValue().toString());
					}

					tag.putLong("marker", context.getBlockPos().asLong());
					stack.setTag(tag);
				}
			} else {
				PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
				buf.writeEnumConstant(context.getHand());
				ServerSidePacketRegistry.INSTANCE.sendToPlayer(context.getPlayer(), LandmarkNetworking.OPEN_CLAIM_SCREEN, buf);
			}
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
				tooltip.add(new TranslatableText("item.landmark.deed.volume", volume, (int)this.maxVolume).styled((style) -> style.withItalic(true).withColor(Formatting.DARK_GRAY)));
			} else {
				tooltip.add(new TranslatableText("item.landmark.deed.volume.creative", volume).styled((style) -> style.withItalic(true).withColor(Formatting.DARK_GRAY)));
			}
		}

		if (tag != null && tag.contains("landmark_name")) {
			tooltip.add(Text.Serializer.fromJson(tag.getString("landmark_name")));
		}

		super.appendTooltip(stack, world, tooltip, context);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		if (!world.isClient) {
			if (user.getStackInHand(hand).hasTag()) {
				verifyTag(user, hand);

				CompoundTag tag = user.getStackInHand(hand).getOrCreateTag();

				if (!tag.contains("landmark_id")) {
					PlayerLandmark landmark = new PlayerLandmark(world);
					LandmarkTrackingComponent.add((ServerWorld) world, landmark.withOwner(user));
					tag.putUuid("landmark_id", landmark.getId());
				}

				PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
				buf.writeEnumConstant(hand);
				buf.writeUuid(tag.getUuid("landmark_id"));
				ServerSidePacketRegistry.INSTANCE.sendToPlayer(user, LandmarkNetworking.OPEN_EDIT_SCREEN, buf);
			} else {
				PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
				buf.writeEnumConstant(hand);
				ServerSidePacketRegistry.INSTANCE.sendToPlayer(user, LandmarkNetworking.OPEN_CLAIM_SCREEN, buf);
			}
		}

		return super.use(world, user, hand);
	}

	public static void verifyTag(PlayerEntity user, Hand hand) {
		CompoundTag tag = user.getStackInHand(hand).getOrCreateTag();
		if (tag.contains("landmark_id") && tag.contains("world_key") && user.getEntityWorld().getServer() != null) {
			ServerWorld world = user.getEntityWorld().getServer().getWorld(RegistryKey.of(Registry.DIMENSION, new Identifier(tag.getString("world_key"))));
			if (LandmarkTrackingComponent.of(world).get(tag.getUuid("landmark_id")) == null) {
				user.setStackInHand(hand, new ItemStack(user.getStackInHand(hand).getItem()));
			}
		}
	}

}
