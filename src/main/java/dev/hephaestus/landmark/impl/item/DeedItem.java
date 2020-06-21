package dev.hephaestus.landmark.impl.item;

import dev.hephaestus.landmark.impl.LandmarkMod;
import dev.hephaestus.landmark.impl.landmarks.LandmarkSection;
import dev.hephaestus.landmark.impl.landmarks.PlayerLandmark;
import dev.hephaestus.landmark.impl.world.LandmarkTrackingComponent;
import dev.hephaestus.landmark.impl.util.Profiler;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.network.PacketContext;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.feature.StructureFeature;

import java.util.*;

public class DeedItem extends Item {
    public static final Identifier DEED_SAVE_PACKET_ID = LandmarkMod.id("deed_screen", "save");
    public static final Identifier DEED_OPEN_EDIT_SCREEN = LandmarkMod.id("deed_screen", "open");

    private final long maxVolume;

    public DeedItem(Settings settings, long maxVolume) {
        super(settings);
        this.maxVolume = maxVolume;
    }

    private static StructureStart<?> overlap(ServerWorld world, BlockBox box) {
        int[][] pos = new int[][] {
                new int[] {box.minX, box.getCenter().getX(), box.maxX},
                new int[] {box.minZ, box.getCenter().getZ(), box.maxZ}
        };

        Set<Chunk> chunks = new HashSet<>();
        for (int x = 0; x < 3; ++x) {
            for (int z = 0; z < 3; ++z) {
                chunks.add(world.getChunk(pos[0][x], pos[1][z]));
            }
        }

        for (Chunk chunk : chunks) {
            Profiler.push("getStructureAccessor");
            StructureAccessor accessor = world.getStructureAccessor();
            Profiler.pop(false);

            Profiler.push("getStructureReferences");
            Map<StructureFeature<?>, LongSet> map = chunk.getStructureReferences();
            Profiler.pop(false);

            Profiler.push("entrySet");
            Set<Map.Entry<StructureFeature<?>, LongSet>> entrySet = map.entrySet();
            Profiler.pop(false);

            Profiler.push("loop");
            for (Map.Entry<StructureFeature<?>, LongSet> e : entrySet) {
                for (Long l : e.getValue()) {
                    Profiler.push("chunkPos");
                    ChunkSectionPos chunkSectionPos = ChunkSectionPos.from(new ChunkPos(l), 0);
                    Profiler.pop(false);

                    Profiler.push("getStructureStart");
                    StructureStart<?> structure = accessor.getStructureStart(chunkSectionPos, e.getKey(), chunk);
                    Profiler.pop(false);

                    if (structure != null) {
                        Profiler.push("intersect");
                        if (box.intersects(structure.getBoundingBox())) return structure;
                        Profiler.pop(false);
                    }
                }
            }
            Profiler.pop(false);
        }

        return null;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (!context.getWorld().isClient && context.getPlayer() != null) {
            ServerWorld world = (ServerWorld) context.getWorld();

            ItemStack stack = context.getPlayer().getStackInHand(context.getHand());
            CompoundTag tag = stack.getOrCreateTag();

            if (tag.contains("marker")) {
                if (RegistryKey.of(Registry.DIMENSION, new Identifier(tag.getString("world_key"))) != world.getRegistryKey()) {
                    context.getPlayer().sendMessage(new TranslatableText("deeds.landmark.fail.world"), true);
                    return ActionResult.FAIL;
                }

                BlockPos marker1 = BlockPos.fromLong(tag.getLong("marker"));
                tag.remove("marker");
                BlockPos marker2 = context.getBlockPos();

                UUID id = tag.getUuid("deed_id");

                BlockBox newBox = new BlockBox(marker1, marker2);

                // This checks that the player's selection overlaps structures. Unfortunately it's very slow and causes lag.
//                StructureStart<?> overlap = overlap(world, newBox);
//                if (overlap != null) {
//                    context.getPlayer().sendMessage(new TranslatableText("deeds.landmark.fail.existing_structure"), true);
//                    return ActionResult.FAIL;
//                }

                PlayerLandmark landmark = (PlayerLandmark) LandmarkTrackingComponent.of(world).get(id);
                if (landmark.add(new LandmarkSection(landmark.getId(), newBox), this.maxVolume)) {
                    double volume = landmark.volume();
                    landmark.makeSections(world);
                    tag.putDouble("volume", volume);
                    context.getPlayer().sendMessage(new TranslatableText("deeds.landmark.success.add_box", volume), true);
                    return ActionResult.SUCCESS;
                } else {
                    context.getPlayer().sendMessage(new TranslatableText("deeds.landmark.fail.volume", this.maxVolume), true);
                    return ActionResult.FAIL;
                }
            } else {
                if (!tag.contains("deed_id")) {
                    PlayerLandmark landmark = new PlayerLandmark(world);
                    LandmarkTrackingComponent.add(world, landmark);
                    tag.putUuid("deed_id", landmark.getId());
                }

                if (!tag.contains("world_key")) {
                    tag.putString("world_key", world.getRegistryKey().getValue().toString());
                }

                tag.putLong("marker", context.getBlockPos().asLong());
                stack.setTag(tag);
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

        tooltip.add(new TranslatableText("item.landmark.deed.volume", volume, this.maxVolume).styled((style) -> style.withItalic(true).withColor(Formatting.DARK_GRAY)));

        if (tag != null && tag.contains("deed_name")) {
            tooltip.add(Text.Serializer.fromJson(tag.getString("deed_name")));
        }

        super.appendTooltip(stack, world, tooltip, context);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        CompoundTag tag = user.getStackInHand(hand).getOrCreateTag();
        if (!world.isClient) {
            if (!tag.contains("deed_id")) {
                PlayerLandmark landmark = new PlayerLandmark(world);
                LandmarkTrackingComponent.add((ServerWorld) world, landmark);
                tag.putUuid("deed_id", landmark.getId());
            }

            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeEnumConstant(hand);
            buf.writeUuid(tag.getUuid("deed_id"));
            ServerSidePacketRegistry.INSTANCE.sendToPlayer(user, DEED_OPEN_EDIT_SCREEN, buf);
        }

        return super.use(world, user, hand);
    }

    public static void saveName(PacketContext context, PacketByteBuf buf) {
        UUID id = buf.readUuid();
        Text name = buf.readText();
        Hand hand = buf.readEnumConstant(Hand.class);

        context.getTaskQueue().execute(() -> {
            ItemStack stack = context.getPlayer().getStackInHand(hand);

            if (stack.getItem() instanceof DeedItem) {
                CompoundTag tag = stack.getOrCreateTag();
                ServerWorld world = (ServerWorld) context.getPlayer().getEntityWorld();

                if (tag.contains("deed_id") && tag.getUuid("deed_id").equals(id)) {
                    tag.putString("deed_name", Text.Serializer.toJson(name));
                    PlayerLandmark landmark = (PlayerLandmark) LandmarkTrackingComponent.of(world).get(id);
                    landmark.setName(name);
                    landmark.makeSections(world);
                }
            }
        });
    }
}
