package dev.hephaestus.landmark.impl.util;

import dev.hephaestus.landmark.impl.LandmarkMod;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.fabricmc.fabric.api.network.PacketContext;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.PersistentState;

import java.util.*;

// The need for this registry is fucking stupid. But apparently Tag
// serialization is so slow that it's just not reasonable to keep all the
// information on the ItemStack itself. So here we are.
public class DeedRegistry extends PersistentState {
    public static final Identifier DEED_REQUEST_PACKET_REQUEST = LandmarkMod.id("deed_registry", "request", "request");
    public static final Identifier DEED_REQUEST_PACKET_RESPONSE = LandmarkMod.id("deed_registry", "request", "response");

    private static final String ID = "deeds";

    private final HashMap<UUID, VoxelShape> boxes = new HashMap<>();
    private final HashMap<UUID, Double> volumes = new HashMap<>();

    public DeedRegistry() {
        super(ID);
    }

    public UUID newDeed() {
        UUID id = UUID.randomUUID();
        return boxes.containsKey(id) ? newDeed() : id;
    }

    public boolean add(UUID id, VoxelShape shape, double maxVolume) {
        VoxelShape newShape = VoxelShapes.union(shape, boxes.computeIfAbsent(id, (x) -> VoxelShapes.empty())).simplify();

        List<Double> volumes = new LinkedList<>();
        newShape.forEachBox((x1, y1, z1, x2, y2, z2) -> volumes.add((x2 - x1) * (y2 - y1) * (z2 - z1)));

        double volume = 0D;
        for (double d : volumes) {
            volume += d;
        }

        if (volume <= maxVolume) {
            this.boxes.put(id, newShape);
            this.volumes.put(id, volume);
            this.markDirty();
            return true;
        }

        return false;
    }

    public double volume(UUID id) {
        return this.volumes.getOrDefault(id, 0D);
    }

    public VoxelShape get(UUID id) {
        return this.boxes.getOrDefault(id, VoxelShapes.empty());
    }

    public VoxelShape remove(UUID id) {
        this.volumes.remove(id);
        VoxelShape result = this.boxes.remove(id);
        this.markDirty();
        return result;
    }

    @Override
    public void fromTag(CompoundTag tag) {
        CompoundTag deedsTag = tag.getCompound("deed_registry");

        CompoundTag boxesTag = deedsTag.getCompound("boxes");
        for (String id : boxesTag.getKeys()) {
            VoxelShape shape = VoxelShapes.empty();
            ListTag list = boxesTag.getList(id, 6);

            for (int i = 0; i < list.size(); i += 6) {
                shape = VoxelShapes.union(shape, VoxelShapes.cuboid(
                        list.getDouble(i),
                        list.getDouble(i + 1),
                        list.getDouble(i + 2),
                        list.getDouble(i + 3),
                        list.getDouble(i + 4),
                        list.getDouble(i + 5)
                ));
            }

            this.boxes.put(UUID.fromString(id), shape);
        }

        CompoundTag volumesTag = deedsTag.getCompound("volumes");
        for (String id : volumesTag.getKeys()) {
            this.volumes.put(UUID.fromString(id), volumesTag.getDouble(id));
        }
    }

    @Override
    public CompoundTag toTag(CompoundTag tag) {
        CompoundTag deedsTag = new CompoundTag();

        CompoundTag boxesTag = new CompoundTag();
        for (Map.Entry<UUID, VoxelShape> entry : this.boxes.entrySet()) {
            ListTag box = new ListTag();

            entry.getValue().forEachBox(((minX, minY, minZ, maxX, maxY, maxZ) -> {
                box.add(DoubleTag.of(minX));
                box.add(DoubleTag.of(minY));
                box.add(DoubleTag.of(minZ));
                box.add(DoubleTag.of(maxX));
                box.add(DoubleTag.of(maxY));
                box.add(DoubleTag.of(maxZ));
            }));

            boxesTag.put(entry.getKey().toString(), box);
        }

        CompoundTag volumesTag = new CompoundTag();
        for (Map.Entry<UUID, Double> entry : this.volumes.entrySet()) {
            volumesTag.putDouble(entry.getKey().toString(), entry.getValue());
        }

        deedsTag.put("boxes", boxesTag);
        deedsTag.put("volumes", volumesTag);
        tag.put("deed_registry", deedsTag);

        return tag;
    }

    public static DeedRegistry get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(DeedRegistry::new, ID);
    }

    @Environment(EnvType.CLIENT)
    public static void request(UUID id) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeUuid(id);
        ClientSidePacketRegistry.INSTANCE.sendToServer(DEED_REQUEST_PACKET_REQUEST, buf);
    }

    public static void response(PacketContext context, PacketByteBuf inBuf) {
        UUID id = inBuf.readUuid();

        context.getTaskQueue().execute(() -> {
            DeedRegistry registry = get(((ServerPlayerEntity) context.getPlayer()).getServerWorld());
            if (registry.boxes.containsKey(id)) {
                PacketByteBuf outBuf = new PacketByteBuf(Unpooled.buffer());
                VoxelShape shape = registry.get(id);

                outBuf.writeUuid(id);
                write(outBuf, shape);

                ServerSidePacketRegistry.INSTANCE.sendToPlayer(context.getPlayer(), DEED_REQUEST_PACKET_RESPONSE, outBuf);
            }
        });
    }

    private static void write(PacketByteBuf buf, VoxelShape shape) {
        buf.writeInt(shape.getBoundingBoxes().size());
        List<Box> boxes = shape.getBoundingBoxes();

        for (Box box : boxes) {
            buf.writeDouble(box.minX);
            buf.writeDouble(box.minY);
            buf.writeDouble(box.minZ);
            buf.writeDouble(box.maxX);
            buf.writeDouble(box.maxY);
            buf.writeDouble(box.maxZ);
        }
    }
}
