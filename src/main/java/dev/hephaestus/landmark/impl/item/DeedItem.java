package dev.hephaestus.landmark.impl.item;

import dev.hephaestus.landmark.api.LandmarkCache;
import dev.hephaestus.landmark.api.LandmarkHolder;
import dev.hephaestus.landmark.impl.Messages;
import dev.hephaestus.landmark.impl.util.ClientHelper;
import dev.hephaestus.landmark.impl.util.Landmark;
import dev.hephaestus.landmark.impl.util.Landmarks;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.CallbackI;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class DeedItem extends Item {
    private final int maxVolume;

    public DeedItem(Settings settings) {
        this(settings, -1);
    }

    public DeedItem(Settings settings, int maxVolume) {
        super(settings);
        this.maxVolume = maxVolume;

        if (maxVolume <= 0 && maxVolume != -1) {
            throw new IllegalArgumentException("Max volume must be positive or -1");
        }
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getPlayer() != null) {
            if (!validate(context.getWorld(), context.getStack())) {
                return ActionResult.PASS;
            } else if (!context.getWorld().isClient) {
                return this.useAt((ServerPlayerEntity) context.getPlayer(), context.getHand(), context.getBlockPos());
            }
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient) {
            ServerPlayerEntity player = (ServerPlayerEntity) user;
            if (player.getStackInHand(hand).hasTag()) {
                if (player.isSneaking()) {
                    HitResult result = player.raycast(player.isCreative() ? 5 : 4.5, 1F, false);

                    if (result instanceof BlockHitResult) {
                        this.useAt(player, hand, ((BlockHitResult) result).getBlockPos());
                    }
                }
            }
        } else {
            ItemStack deed = user.getStackInHand(hand);
            int slot = hand == Hand.MAIN_HAND ? user.inventory.selectedSlot : 40;

            if (deed.hasTag()) {
                if (user.isSneaking()) return TypedActionResult.success(deed);

                UUID landmarkId = deed.getOrCreateTag().getUuid("landmark_id");
                LandmarkCache cache = LandmarkCache.getInstance(world);
                Landmark landmark = cache.getLandmark(landmarkId);

                if (!landmark.isOwnedBy(user.getUuid())) {
                    user.sendMessage(Messages.OWNED, true);
                    return TypedActionResult.success(deed);
                }
            }

            ClientHelper.openScreen(deed, user, slot);

            return TypedActionResult.success(deed);
        }

        return super.use(world, user, hand);
    }

    private ActionResult useAt(ServerPlayerEntity player, Hand hand, BlockPos pos) {
        ServerWorld world = player.getServerWorld();
        ItemStack deed = player.getStackInHand(hand);

        if (!validate(world, deed)) return ActionResult.PASS;

        CompoundTag tag = deed.getOrCreateTag();

        UUID landmarkId = tag.getUuid("landmark_id");
        Landmarks landmarks = Landmarks.of(world);
        Landmark landmark = landmarks.getLandmark(landmarkId);

        if (!landmark.isOwnedBy(player.getUuid())) {
            player.sendMessage(Messages.OWNED, true);
            return ActionResult.FAIL;
        }

        if (tag.contains("marker", NbtType.LONG)) {
            BlockPos marker = BlockPos.fromLong(tag.getLong("marker"));

            TypedActionResult<@Nullable Text> result = landmarks.add(landmarkId, new BlockBox(marker, pos), this.maxVolume);

            tag.remove("marker");

            if (!result.getResult().isAccepted()) {
                player.sendMessage(result.getValue(), true);
            }

            return result.getResult();
        } else {
            tag.putLong("marker", pos.asLong());
            return ActionResult.SUCCESS;
        }
    }

    private static boolean validate(World world, ItemStack deed) {
        CompoundTag tag = deed.getTag();

        if (tag != null) {
            return tag.contains("world", NbtType.STRING)
                    && world.getRegistryKey().getValue().toString().equals(tag.getString("world"))
                    && tag.contains("landmark_id", NbtType.INT_ARRAY)
                    && LandmarkHolder.getInstance(world).contains(tag.getUuid("landmark_id"));
        } else {
            return false;
        }
    }

    private static BlockPos traceForBlock(ServerPlayerEntity player) {
        double d = 4;
        Vec3d angle = player.getRotationVec(1F);
        return player.world.raycast(new RaycastContext(
                player.getCameraPosVec(1F),
                player.getCameraPosVec(1F).add(d * angle.x, d * angle.y, d * angle.z),
                RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, player
        )).getBlockPos();
    }

    public int getMaxVolume() {
        return this.maxVolume;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);


        if (!world.isClient && stack.hasTag()) {
            CompoundTag tag = stack.getOrCreateTag();

            Landmark landmark = Landmarks.of((ServerWorld) world).getLandmark(tag.getUuid("landmark_id"));
            tag.putInt("volume", landmark.getVolume());

            Collection<ChunkPos> chunks = landmark.getChunks();

            if (!chunks.isEmpty()) {
                BlockPos center = landmark.getCenter();
                tag.putIntArray("location", new int[] {center.getX(), center.getX(), center.getZ()});
            }

            stack.setCustomName(landmark.getName());
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        Style style = Style.EMPTY.withColor(Formatting.DARK_GRAY);
        DeedItem deed = ((DeedItem) stack.getItem());

        if (stack.hasTag()) {
            CompoundTag tag = stack.getOrCreateTag();

            if (tag.contains("landmark_id", NbtType.INT_ARRAY)) {
                tooltip.add(new LiteralText(tag.getUuid("landmark_id").toString())
                        .setStyle(style));
            }

            if (tag.contains("world", NbtType.STRING)) {
                tooltip.add(new TranslatableText("item.landmark.deed.world", tag.getString("world"))
                        .setStyle(style));
            }

            if (tag.contains("location", NbtType.INT_ARRAY)) {
                int[] location = tag.getIntArray("location");
                tooltip.add(new TranslatableText("item.landmark.deed.location", location[0], location[1], location[2])
                        .setStyle(style));
            }

            int volume = tag.contains("volume", NbtType.INT) ? tag.getInt("volume") : 0;

            if (deed.maxVolume == -1) {
                tooltip.add(new TranslatableText("item.landmark.deed.volume.creative", volume)
                        .setStyle(style));
            } else {
                tooltip.add(new TranslatableText("item.landmark.deed.volume", volume, deed.maxVolume)
                        .setStyle(style));
            }
        } else {
            tooltip.add(new TranslatableText("item.landmark.deed.volume", 0, deed.maxVolume)
                    .setStyle(style));
        }
    }
}
