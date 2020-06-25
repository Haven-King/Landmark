package dev.hephaestus.landmark.impl.item;

import dev.hephaestus.landmark.impl.network.LandmarkNetworking;
import io.netty.buffer.Unpooled;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;

public class EvictionNoticeItem extends Item {
	public EvictionNoticeItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		if (!world.isClient) {
			ServerSidePacketRegistry.INSTANCE.sendToPlayer(user, LandmarkNetworking.OPEN_DELETION_SCREEN, new PacketByteBuf(Unpooled.buffer()));
		}

		return TypedActionResult.success(user.getStackInHand(hand));
	}
}
