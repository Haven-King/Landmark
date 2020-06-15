package dev.hephaestus.landmark.impl.util;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.PersistentState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A simple synced PersistentState.
 */
public abstract class SyncedState extends PersistentState {
    protected static final Logger LOGGER = LogManager.getLogger();
    protected final Identifier packetSyncId;
    protected final MinecraftServer server;
    private CompoundTag tag;

    /**
     * @param key the key to be used for the PersistentState
     * @param server used for sending updates. Should be null on clients
     */
    public SyncedState(String key, Identifier packetSyncId, MinecraftServer server) {
        super(key);
        this.packetSyncId = packetSyncId;
        this.server = server;
    }

    protected void setTag(CompoundTag tag) {
        this.tag = tag;
    }

    @Override
    public void markDirty() {
        super.markDirty();
        this.setTag(new CompoundTag());
        this.sync();
    }

    protected final void sync() {
        if (this.server != null) {
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeCompoundTag(tag);

            for (ServerPlayerEntity player : this.server.getPlayerManager().getPlayerList()) {
                ServerSidePacketRegistry.INSTANCE.sendToPlayer(player, this.packetSyncId, buf);
            }
        }
    }

    public final void send(ServerPlayerEntity player) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeCompoundTag(tag);

        ServerSidePacketRegistry.INSTANCE.sendToPlayer(player, this.packetSyncId, buf);
    }

    @Override
    public final CompoundTag toTag(CompoundTag tag) {
        return this.tag;
    }
}
