package dev.hephaestus.landmark.impl.util;

import dev.hephaestus.landmark.api.LandmarkType;
import dev.hephaestus.landmark.api.LandmarkTypeRegistry;
import dev.hephaestus.landmark.api.LandmarkView;
import dev.hephaestus.landmark.impl.Messages;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.nbt.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.gen.feature.FeatureConfig;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;

public class Landmark implements LandmarkView {
    private static final int MARGIN = 5;

    private final UUID id;
    private final Collection<BlockBox> boxes;
    private final Collection<ChunkPos> chunks;
    private final Collection<UUID> owners;
    private final Vector3f color;

    private Text name = LiteralText.EMPTY;
    private VoxelShape shape = VoxelShapes.empty();
    private int volume;

    public Landmark(UUID id, UUID owner) {
        this.id = id;
        this.boxes = new ArrayList<>();
        this.chunks = new HashSet<>();
        this.owners = new HashSet<>();
        this.owners.add(owner);
        this.color = new Vector3f(1, 1, 1);
    }

    private Landmark(UUID id, Collection<BlockBox> boxes, Collection<ChunkPos> chunks, Collection<UUID> owners, Text name, Vector3f color, VoxelShape shape) {
        this.id = id;
        this.boxes = boxes;
        this.chunks = chunks;
        this.owners = owners;
        this.name = name;
        this.color = color;
        this.shape = shape;

        for (Box box1 : shape.getBoundingBoxes()) {
            this.volume += volume(box1);
        }
    }

    public static <T extends FeatureConfig> @Nullable Landmark of(UUID id, StructureStart<T> structureStart, ServerWorld world) {
        LandmarkType type = LandmarkTypeRegistry.get(structureStart, world);

        if (type == null) return null;

        Collection<BlockBox> boxes = new ArrayList<>();

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (StructurePiece piece : structureStart.getChildren()) {
            BlockBox box = new BlockBox(piece.getBoundingBox());
            boxes.add(box);
            minX = Math.min(minX, box.minX - MARGIN);
            minY = Math.min(minY, box.minY);
            minZ = Math.min(minZ, box.minZ - MARGIN);
            maxX = Math.max(maxX, box.maxX + MARGIN);
            maxY = Math.max(maxY, box.maxY);
            maxZ = Math.max(maxZ, box.maxZ + MARGIN);
        }

        VoxelShape shape = VoxelShapes.empty();

        for (BlockBox box : boxes) {
            box.minY = minY;
            box.maxY = maxY;
            box.minX -= MARGIN;
            box.minZ -= MARGIN;
            box.maxX += MARGIN;
            box.maxZ += MARGIN;

            shape = VoxelShapes.union(
                    shape,
                    VoxelShapes.cuboid(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ)
            );
        }

        boxes = from(shape);

        Collection<ChunkPos> chunks = new HashSet<>();
        for (int x = minX >> 4; x <= maxX >> 4; ++x) {
            for (int z = minZ >> 4; z <= maxZ >> 4; ++z) {
                chunks.add(new ChunkPos(x, z));
            }
        }

        Text name = type.generateName();
        TextColor textColor = name.getStyle().getColor();

        Vector3f color;

        if (textColor == null) {
            color = new Vector3f(1, 1, 1);
        } else {
            int i = textColor.getRgb();
            color = new Vector3f(
                    ((float) ((i >>> 16) & 0xFF)) / 255F,
                    ((float) ((i >>> 8) & 0xFF)) / 255F,
                    ((float) (i & 0xFF)) / 255F
            );
        }

        return new Landmark(
                id, boxes,
                chunks,
                new HashSet<>(),
                name,
                color,
                shape);
    }

    @Override
    public UUID getId() {
        return this.id;
    }

    @Override
    public Collection<BlockBox> getBoxes() {
        return this.boxes;
    }

    @Override
    public Collection<ChunkPos> getChunks() {
        return this.chunks;
    }

    @Override
    public Text getName() {
        return this.name;
    }

    @Override
    public Vector3f getColor() {
        return this.color;
    }

    @Override
    public boolean contains(Vec3i pos) {
        for (BlockBox box : this.boxes) {
            if (box.contains(pos)) return true;
        }

        return false;
    }

    @Override
    public boolean isOwnedBy(UUID playerId) {
        return this.owners.contains(playerId);
    }

    @Override
    public boolean isClaimed() {
        return this.owners.size() > 0;
    }

    @Override
    public int getVolume() {
        return this.volume;
    }

    @Override
    public BlockPos getCenter() {
        double x = shape.getMin(Direction.Axis.X) + (shape.getMax(Direction.Axis.X) - shape.getMin(Direction.Axis.X)) / 2;
        double y = shape.getMin(Direction.Axis.Y) + (shape.getMax(Direction.Axis.Y) - shape.getMin(Direction.Axis.Y)) / 2;
        double z = shape.getMin(Direction.Axis.Z) + (shape.getMax(Direction.Axis.Z) - shape.getMin(Direction.Axis.Z)) / 2;

        return new BlockPos(x, y, z);
    }

    public CompoundTag toTag() {
        CompoundTag landmarkTag = new CompoundTag();

        landmarkTag.putUuid("id", this.id);
        landmarkTag.putString("name", Text.Serializer.toJson(this.name));

        CompoundTag color = new CompoundTag();
        color.putFloat("r", this.color.getX());
        color.putFloat("g", this.color.getY());
        color.putFloat("b", this.color.getZ());

        landmarkTag.put("color", color);

        ListTag boxes = new ListTag();

        for (BlockBox box : this.boxes) {
            IntArrayTag boxTag = new IntArrayTag(new int[] {
                    box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ
            });

            boxes.add(boxTag);
        }

        ListTag chunks = new ListTag();

        for (ChunkPos pos : this.chunks) {
            IntArrayTag posTag = new IntArrayTag(new int[] {
                    pos.x, pos.z
            });

            chunks.add(posTag);
        }

        ListTag owners = new ListTag();

        for (UUID owner : this.owners) {
            owners.add(NbtHelper.fromUuid(owner));
        }

        landmarkTag.put("boxes", boxes);
        landmarkTag.put("chunks", chunks);
        landmarkTag.put("owners", owners);

        return landmarkTag;
    }

    public static Landmark fromTag(CompoundTag landmarkTag) {
        UUID id = landmarkTag.getUuid("id");
        Text name = Text.Serializer.fromJson(landmarkTag.getString("name"));

        CompoundTag colorTag = landmarkTag.getCompound("color");
        Vector3f color = new Vector3f(
                colorTag.getFloat("r"),
                colorTag.getFloat("g"),
                colorTag.getFloat("b")
        );

        Collection<BlockBox> boxes = new ArrayList<>();

        VoxelShape shape = VoxelShapes.empty();

        for (Tag t : landmarkTag.getList("boxes", NbtType.INT_ARRAY)) {
            IntArrayTag boxTag = (IntArrayTag) t;

            int minX = boxTag.get(0).getInt(), minY = boxTag.get(1).getInt(), minZ = boxTag.get(2).getInt();
            int maxX = boxTag.get(3).getInt(), maxY = boxTag.get(4).getInt(), maxZ = boxTag.get(5).getInt();

            boxes.add(new BlockBox(minX, minY, minZ, maxX, maxY, maxZ));
            shape = VoxelShapes.union(shape, VoxelShapes.cuboid(
                    minX, minY, minZ, maxX, maxY, maxZ
            ));
        }

        Collection<ChunkPos> chunks = new HashSet<>();

        for (Tag t : landmarkTag.getList("chunks", NbtType.INT_ARRAY)) {
            IntArrayTag boxTag = (IntArrayTag) t;

            chunks.add(new ChunkPos(
                    boxTag.get(0).getInt(),
                    boxTag.get(1).getInt()
            ));
        }

        Collection<UUID> owners = new HashSet<>();

        for (Tag t : landmarkTag.getList("owners", NbtType.INT_ARRAY)) {
            owners.add(NbtHelper.toUuid(t));
        }

        return new Landmark(id, boxes, chunks, owners, name, color, shape);
    }

    TypedActionResult<@Nullable Text> add(BlockBox box, int maxVolume) {
        if (!this.boxes.isEmpty()) {
            boolean intersects = false;

            for (BlockBox existing : this.boxes) {
                if (existing.intersects(box) || isTouching(existing, box)) {
                    intersects = true;
                    break;
                }
            }

            if (!intersects) {
                return TypedActionResult.fail(Messages.NOT_CONNECTED);
            }
        }

        for (int x = box.minX >> 4; x <= box.maxX >> 4; ++x) {
            for (int z = box.minZ >> 4; z <= box.maxZ >> 4; ++z) {
                this.chunks.add(new ChunkPos(x, z));
            }
        }

        VoxelShape shape = VoxelShapes.union(this.shape, VoxelShapes.cuboid(
                box.minX,
                box.minY,
                box.minZ,
                box.maxX + 1,
                box.maxY + 1,
                box.maxZ + 1
        ));

        int volume = 0;

        for (Box box1 : shape.getBoundingBoxes()) {
            volume += volume(box1);
        }

        if (volume > maxVolume && maxVolume != -1) {
            return TypedActionResult.fail(new TranslatableText("error.landmark.too_large", volume, maxVolume));
        } else {
            this.volume = volume;
        }

        this.shape = shape;

        this.boxes.clear();
        this.boxes.addAll(from(this.shape));

        return TypedActionResult.success(null);
    }

    void setName(Text name) {
        this.name = name;
    }

    void setColor(float r, float g, float b) {
        this.color.set(r, g, b);
    }

    boolean addOwner(UUID uuid) {
        return this.owners.add(uuid);
    }

    private static boolean intersects(BlockBox a, BlockBox b) {
        return intersects2(a, b) || intersects2(b, a);
    }

    private static boolean isTouching(BlockBox a, BlockBox b) {
        return (a.minX == b.maxX + 1 && yzOverlap(a, b))
                || (a.minY == b.maxY + 1 && xzOverlap(a, b))
                || (a.minZ == b.maxZ + 1 && xyOverlap(a, b))
                || (a.maxX == b.minX && yzOverlap(a, b))
                || (a.maxY == b.minY && xzOverlap(a, b))
                || (a.maxZ == b.minZ && xyOverlap(a, b));
    }

    private static boolean xyOverlap(BlockBox a, BlockBox b) {
        return (a.minX < b.maxX + 1 && a.maxX > b.minX && a.maxY > b.minY && a.minY < b.maxY + 1);
    }

    private static boolean xzOverlap(BlockBox a, BlockBox b) {
        return (a.minX < b.maxX + 1 && a.maxX > b.minX && a.maxZ > b.minZ && a.minZ < b.maxZ + 1);
    }

    private static boolean yzOverlap(BlockBox a, BlockBox b) {
        return (a.minZ < b.maxZ + 1 && a.maxZ > b.minZ && a.maxY > b.minY && a.minY < b.maxY + 1);
    }

    private static boolean intersects2(BlockBox a, BlockBox b) {
        return (a.minX <= b.maxX && a.maxX >= b.minX) &&
                (a.minY <= b.maxY && a.maxY >= b.minY) &&
                (a.minZ <= b.maxZ && a.maxZ >= b.minZ);
    }

    private static Collection<BlockBox> from(VoxelShape shape) {
        Collection<BlockBox> boxes = new ArrayList<>();

        for (Box box : shape.getBoundingBoxes()) {
            boxes.add(new BlockBox(
                    MathHelper.floor(box.minX),
                    MathHelper.floor(box.minY),
                    MathHelper.floor(box.minZ),
                    MathHelper.ceil(box.maxX),
                    MathHelper.ceil(box.maxY),
                    MathHelper.ceil(box.maxZ)));
        }

        return boxes;
    }

    private static int volume(Box box) {
        return MathHelper.ceil(box.getXLength() * box.getYLength() * box.getZLength());
    }
}
