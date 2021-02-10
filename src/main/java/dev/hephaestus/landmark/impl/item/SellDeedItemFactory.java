package dev.hephaestus.landmark.impl.item;

import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOffers;

import java.util.Random;

public class SellDeedItemFactory implements TradeOffers.Factory {
    private final Item sell;
    private final int price;
    private final int count;
    private final int maxUses;
    private final int experience;
    private final float multiplier;

    public SellDeedItemFactory(Item sell, int price, int count, int maxUses, int experience, float multiplier) {
        this.sell = sell;
        this.price = price;
        this.count = count;
        this.maxUses = maxUses;
        this.experience = experience;
        this.multiplier = multiplier;
    }

    public TradeOffer create(Entity entity, Random random) {
        return new TradeOffer(new ItemStack(Items.EMERALD, Math.min(this.price - random.nextInt(MathHelper.floor(Math.sqrt(this.price))), this.price / 2)), new ItemStack(this.sell, this.count), this.maxUses, this.experience, this.multiplier);
    }
}
