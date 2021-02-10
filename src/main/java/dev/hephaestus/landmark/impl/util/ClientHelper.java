package dev.hephaestus.landmark.impl.util;

import dev.hephaestus.landmark.impl.client.screen.ClaimScreen;
import dev.hephaestus.landmark.impl.client.screen.EditScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public class ClientHelper {
    public static void openScreen(ItemStack deed, PlayerEntity user, int slot) {
        MinecraftClient.getInstance().openScreen(deed.hasTag()
                ? new EditScreen((ClientPlayerEntity) user, slot)
                : new ClaimScreen((ClientPlayerEntity) user, slot)
        );
    }
}
