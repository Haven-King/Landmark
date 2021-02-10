package dev.hephaestus.landmark.impl;

import dev.hephaestus.landmark.api.LandmarkCache;
import dev.hephaestus.landmark.api.LandmarkView;
import dev.hephaestus.landmark.impl.client.LandmarkCacheImpl;
import dev.hephaestus.landmark.impl.client.screen.EditScreen;
import dev.hephaestus.landmark.impl.item.DeedItem;
import dev.hephaestus.landmark.impl.util.Landmark;
import dev.hephaestus.landmark.impl.util.Landmarks;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.BackgroundHelper;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.UUID;

public class LandmarkNetworking {
    public static final Identifier LANDMARK_INFO = packet(Type.S2C, "landmark", "info");
    public static final Identifier INITIALIZE_CACHE = packet(Type.S2C, "landmark", "init_cache");
    public static final Identifier OPEN_EDIT_SCREEN = packet(Type.S2C, "landmark", "screen", "open_edit");
    public static final Identifier CLOSE_SCREEN = packet(Type.S2C, "landmark", "screen", "close");

    public static final Identifier NEW_LANDMARK = packet(Type.C2S, "landmark", "new");
    public static final Identifier SET_NAME = packet(Type.C2S, "landmark", "set_name");
    public static final Identifier CLAIM_LANDMARK = packet(Type.C2S, "landmark", "claim");

    public static void init() {
        ServerPlayConnectionEvents.INIT.register((handler, server) ->  {
            ServerPlayNetworking.registerReceiver(handler, NEW_LANDMARK, LandmarkNetworking::newLandmark);
            ServerPlayNetworking.registerReceiver(handler, CLAIM_LANDMARK, LandmarkNetworking::claimLandmark);
            ServerPlayNetworking.registerReceiver(handler, SET_NAME, LandmarkNetworking::setName);
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            ChunkPos pos = new ChunkPos(handler.player.getBlockPos());
            Collection<Landmark> landmarks = Landmarks.of(handler.player.getServerWorld()).getLandmarks(pos);

            buf.writeIdentifier(handler.player.world.getRegistryKey().getValue());
            buf.writeVarInt(landmarks.size());

            for (Landmark landmark : landmarks) {
                buf.writeCompoundTag(landmark.toTag());
            }

            sender.sendPacket(INITIALIZE_CACHE, buf);
        });
    }

    @Environment(EnvType.CLIENT)
    public static void initClient() {
        ClientPlayConnectionEvents.INIT.register((handler, client) -> {
            ClientPlayNetworking.registerReceiver(LANDMARK_INFO, LandmarkNetworking::receiveLandmark);
            ClientPlayNetworking.registerReceiver(INITIALIZE_CACHE, LandmarkNetworking::initializeCache);
            ClientPlayNetworking.registerReceiver(OPEN_EDIT_SCREEN, LandmarkNetworking::openEditScreen);
            ClientPlayNetworking.registerReceiver(CLOSE_SCREEN, LandmarkNetworking::closeScreen);
        });
    }

    @Environment(EnvType.CLIENT)
    private static void receiveLandmark(MinecraftClient client, ClientPlayNetworkHandler clientPlayNetworkHandler, PacketByteBuf buf, PacketSender packetSender) {
        CompoundTag landmarkTag = buf.readCompoundTag();
        RegistryKey<World> world = RegistryKey.of(Registry.DIMENSION, buf.readIdentifier());

        if (landmarkTag == null) {
            LandmarkMod.LOG.warn("Failed to read landmark from packet: landmark was null.");
            return;
        }

        LandmarkMod.EXECUTOR.execute(() -> {
            Landmark landmark = Landmark.fromTag(landmarkTag);
            client.execute(() -> LandmarkCache.getInstance(world).cache(landmark));
        });
    }

    @Environment(EnvType.CLIENT)
    private static void initializeCache(MinecraftClient client, ClientPlayNetworkHandler clientPlayNetworkHandler, PacketByteBuf buf, PacketSender packetSender) {
        RegistryKey<World> world = RegistryKey.of(Registry.DIMENSION, buf.readIdentifier());
        int landmarks = buf.readVarInt();
        LandmarkCache cache = LandmarkCache.getInstance(world);

        for (int i = 0; i < landmarks; ++i) {
            //noinspection ConstantConditions
            cache.cache(Landmark.fromTag(buf.readCompoundTag()));
        }

        ((LandmarkCacheImpl) cache).initialize();
    }

    public static void send(ServerWorld world, Collection<ServerPlayerEntity> players, Landmark landmark) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer()).writeCompoundTag(landmark.toTag());
        buf.writeIdentifier(world.getRegistryKey().getValue());
        players.forEach(player -> ServerPlayNetworking.send(player, LANDMARK_INFO, buf));
    }

    public static void send(ServerWorld world, ChunkPos pos, ServerPlayerEntity player) {
        Landmarks.of(world).getLandmarks(pos).forEach(landmark -> {
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer()).writeCompoundTag(landmark.toTag());
            buf.writeIdentifier(world.getRegistryKey().getValue());
            ServerPlayNetworking.send(player, LANDMARK_INFO, buf);
        });
    }

    public static void send(ServerWorld world, Landmark landmark) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer()).writeCompoundTag(landmark.toTag());
        buf.writeIdentifier(world.getRegistryKey().getValue());

        PlayerLookup.world(world).forEach(player -> ServerPlayNetworking.send(player, LANDMARK_INFO, buf));
    }

    @Environment(EnvType.CLIENT)
    public static void newLandmark(int slot) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());

        buf.writeVarInt(slot);

        ClientPlayNetworking.send(NEW_LANDMARK, buf);
    }

    @Environment(EnvType.CLIENT)
    public static void claimLandmark(UUID landmarkId, int slot) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());

        buf.writeUuid(landmarkId);
        buf.writeVarInt(slot);

        ClientPlayNetworking.send(CLAIM_LANDMARK, buf);
    }

    private static void newLandmark(MinecraftServer server, ServerPlayerEntity playerEntity, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender packetSender) {
        int slot = buf.readVarInt();

        server.execute(() -> {
            ItemStack stack = playerEntity.inventory.getStack(slot);
            if (stack.getItem() instanceof DeedItem && !stack.hasTag()) {
                Landmark landmark = Landmarks.of(playerEntity.getServerWorld()).newLandmark(playerEntity.getUuid());

                CompoundTag tag = stack.getOrCreateTag();

                tag.putString("world", playerEntity.world.getRegistryKey().getValue().toString());
                tag.putUuid("landmark_id", landmark.getId());

                packetSender.sendPacket(OPEN_EDIT_SCREEN, new PacketByteBuf(Unpooled.buffer())
                        .writeCompoundTag(landmark.toTag()));
            } else {
                packetSender.sendPacket(CLOSE_SCREEN, new PacketByteBuf(Unpooled.EMPTY_BUFFER));
            }
        });
    }

    private static void claimLandmark(MinecraftServer server, ServerPlayerEntity playerEntity, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender packetSender) {
        UUID id = buf.readUuid();
        int slot = buf.readVarInt();

        server.execute(() -> {
            ItemStack stack = playerEntity.inventory.getStack(slot);
            if (stack.getItem() instanceof DeedItem && !stack.hasTag()) {
                Landmarks landmarks = Landmarks.of(playerEntity.getServerWorld());
                Landmark landmark = landmarks.getLandmark(id);

                if (landmark == null) {
                    playerEntity.sendMessage(Messages.MISSING, true);
                    packetSender.sendPacket(CLOSE_SCREEN, new PacketByteBuf(Unpooled.EMPTY_BUFFER));
                    return;
                }

                UUID player = playerEntity.getUuid();

                if (landmark.isClaimed() && !landmark.isOwnedBy(player)) {
                    playerEntity.sendMessage(Messages.OWNED, true);
                    packetSender.sendPacket(CLOSE_SCREEN, new PacketByteBuf(Unpooled.EMPTY_BUFFER));
                    return;
                }

                int volume = landmark.getVolume();
                int maxVolume = ((DeedItem) stack.getItem()).getMaxVolume();

                if (landmark.getVolume() > ((DeedItem) stack.getItem()).getMaxVolume()) {
                    playerEntity.sendMessage(new TranslatableText("error.landmark.too_large", volume, maxVolume), true);
                    packetSender.sendPacket(CLOSE_SCREEN, new PacketByteBuf(Unpooled.EMPTY_BUFFER));
                    return;
                }

                if (!landmark.isClaimed()) {
                    landmarks.addOwner(id, player);
                }

                CompoundTag tag = stack.getOrCreateTag();
                tag.putString("world", playerEntity.world.getRegistryKey().getValue().toString());
                tag.putUuid("landmark_id", landmark.getId());
                tag.putInt("volume", landmark.getVolume());

                packetSender.sendPacket(OPEN_EDIT_SCREEN, new PacketByteBuf(Unpooled.buffer())
                        .writeCompoundTag(landmark.toTag()));
            }

            packetSender.sendPacket(CLOSE_SCREEN, new PacketByteBuf(Unpooled.EMPTY_BUFFER));
        });
    }

    @Environment(EnvType.CLIENT)
    public static void setName(UUID id, Text name) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer()).writeUuid(id).writeText(name);
        ClientPlayNetworking.send(SET_NAME, buf);
    }

    private static void setName(MinecraftServer server, ServerPlayerEntity playerEntity, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender packetSender) {
        UUID id = buf.readUuid();
        Text text = buf.readText();

        server.execute(() -> {
            Landmarks landmarks = Landmarks.of(playerEntity.getServerWorld());
            LandmarkView landmark = landmarks.getLandmark(id);

            if (!landmark.isOwnedBy(playerEntity)) {
                playerEntity.sendMessage(Messages.OWNED, true);
                return;
            }

            TextColor color = text.getStyle().getColor();

            if (color == null) {
                LandmarkMod.LOG.warn("Failed to set name: Color was null");
                return;
            }

            int c = text.getStyle().getColor().getRgb();

            float r = BackgroundHelper.ColorMixer.getRed(c) / 255F;
            float g = BackgroundHelper.ColorMixer.getGreen(c) / 255F;
            float b = BackgroundHelper.ColorMixer.getBlue(c) / 255F;

            boolean bl1 = landmarks.setName(id, text);
            boolean bl2 = landmarks.setColor(id, r, g, b);

            if (bl1 || bl2) {
                LandmarkNetworking.send(playerEntity.getServerWorld(), landmarks.getLandmark(id));
            }
        });
    }

    public static void send(ServerWorld world, ServerPlayerEntity player) {
        Landmarks.of(world).getLandmarksWithoutChunks().forEach(landmark -> {
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer()).writeCompoundTag(landmark.toTag());
            buf.writeIdentifier(world.getRegistryKey().getValue());
            ServerPlayNetworking.send(player, LANDMARK_INFO, buf);
        });
    }

    @Environment(EnvType.CLIENT)
    private static void closeScreen(MinecraftClient client, ClientPlayNetworkHandler clientPlayNetworkHandler, PacketByteBuf buf, PacketSender packetSender) {
        client.execute(() -> client.openScreen(null));
    }

    @Environment(EnvType.CLIENT)
    private static void openEditScreen(MinecraftClient client, ClientPlayNetworkHandler clientPlayNetworkHandler, PacketByteBuf buf, PacketSender packetSender) {
        if (client.player != null && client.world != null) {
            Landmark landmark = Landmark.fromTag(buf.readCompoundTag());

            client.execute(() -> {
                LandmarkCache.getInstance(client.world).cache(landmark);
                client.openScreen(new EditScreen(client.player, landmark));
            });
        }
    }

    private static Identifier packet(Type type, String... path) {
        return LandmarkMod.id("packet/" + type.string + "/" + String.join("/", path));
    }

    enum Type {
        C2S("c2s"), S2C("s2c");

        public final String string;

        Type(String string) {
            this.string = string;
        }
    }
}
