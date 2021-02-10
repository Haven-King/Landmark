package dev.hephaestus.landmark.impl;

import dev.hephaestus.landmark.api.LandmarkTypeRegistry;
import dev.hephaestus.landmark.impl.item.DeedItem;
import dev.hephaestus.landmark.impl.item.SellDeedItemFactory;
import dev.hephaestus.landmark.impl.names.NameGenerator;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.fabricmc.fabric.api.tag.TagRegistry;
import net.minecraft.item.Item;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.VillagerProfession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class LandmarkMod implements ModInitializer {
    public static final String MOD_ID = "landmark";
    public static final Logger LOG = LogManager.getLogger();

    public static final Item COMMON_DEED = new DeedItem(new FabricItemSettings().rarity(Rarity.COMMON), 2048);
    public static final Item UNCOMMON_DEED = new DeedItem(new FabricItemSettings().rarity(Rarity.UNCOMMON), 8192);
    public static final Item RARE_DEED = new DeedItem(new FabricItemSettings().rarity(Rarity.RARE), 32768);
    public static final Item EPIC_DEED = new DeedItem(new FabricItemSettings().rarity(Rarity.EPIC), 262144);
    public static final Item CREATIVE_DEED = new DeedItem(new FabricItemSettings().rarity(Rarity.EPIC));

    public static final Tag<Item> SHOWS_BOUNDS = TagRegistry.item(id("shows_bounds"));

    static {
        Registry.register(Registry.ITEM, id("common_deed"), COMMON_DEED);
        Registry.register(Registry.ITEM, id("uncommon_deed"), UNCOMMON_DEED);
        Registry.register(Registry.ITEM, id("rare_deed"), RARE_DEED);
        Registry.register(Registry.ITEM, id("epic_deed"), EPIC_DEED);
        Registry.register(Registry.ITEM, id("creative_deed"), CREATIVE_DEED);

        VillagerProfession[] professions = new VillagerProfession[] {
                VillagerProfession.CLERIC, VillagerProfession.LIBRARIAN
        };

        for (VillagerProfession profession : professions) {
            TradeOfferHelper.registerVillagerOffers(profession, 2, list -> {
                list.add(new SellDeedItemFactory(COMMON_DEED, 5, 1, 12, 5, 0.05F));
            });
        }

        for (VillagerProfession profession : professions) {
            TradeOfferHelper.registerVillagerOffers(profession, 2, list -> {
                list.add(new SellDeedItemFactory(UNCOMMON_DEED, 15, 1, 7, 10, 0.05F));
            });
        }

        for (VillagerProfession profession : professions) {
            TradeOfferHelper.registerVillagerOffers(profession, 3, list -> {
                list.add(new SellDeedItemFactory(RARE_DEED, 35, 1, 4, 13, 0.05F));
            });
        }

        for (VillagerProfession profession : professions) {
            TradeOfferHelper.registerVillagerOffers(profession, 4, list -> {
                list.add(new SellDeedItemFactory(EPIC_DEED, 64, 1, 1, 19, 0.05F));
            });
        }
    }

    private static int i = 0;
    public static final Executor EXECUTOR = Executors.newFixedThreadPool(8, r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName("Landmark-Worker-" + i++);

        return t;
    });

    public static Identifier id(String... path) {
        return new Identifier(MOD_ID, String.join("/", path));
    }

    @Override
    public void onInitialize() {
        NameGenerator.init();
        LandmarkTypeRegistry.init();
        LandmarkNetworking.init();
    }
}
