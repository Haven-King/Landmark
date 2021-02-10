package dev.hephaestus.landmark.impl.client;

import dev.hephaestus.landmark.api.LandmarkCache;
import dev.hephaestus.landmark.api.LandmarkType;
import dev.hephaestus.landmark.api.LandmarkTypeRegistry;
import dev.hephaestus.landmark.impl.LandmarkConfig;
import dev.hephaestus.landmark.impl.client.util.DrawableExtensions;
import dev.hephaestus.landmark.impl.util.Landmark;
import dev.hephaestus.landmark.api.LandmarkHolder;
import dev.hephaestus.landmark.impl.util.LandmarkHandlerHolder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public class LandmarkHandler implements DrawableExtensions {
    private Text mainText = null;
    private Text subText = null;
    private float startTime;

    private final Map<UUID, Integer> landmarkStatuses = new ConcurrentHashMap<>();
    private final Map<Identifier, Boolean> nonFeatureLandmarkStatuses = new ConcurrentHashMap<>();

    public static void init() {
        HudRenderCallback.EVENT.register((matrices, tickDelta) -> {
            ClientWorld world = MinecraftClient.getInstance().world;
            if (world != null) {
                ((LandmarkHandlerHolder) world).getLandmarkHandler().render(matrices, tickDelta);
            }
        });
    }

    public void render(MatrixStack matrices, float tickDelta) {
        if (mainText != null) {
            ClientWorld world = MinecraftClient.getInstance().world;

            if (world != null) {
                TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

                float time = world.getTime();
                float range = (time + tickDelta - startTime);

                if (time + tickDelta > startTime + LandmarkConfig.NOTIFICATION_TIME) {
                    clear();
                    return;
                }

                double progress = range / LandmarkConfig.NOTIFICATION_TIME;

                float m = 0.1F;

                int alpha = MathHelper.ceil(255 * (progress <= m
                        ? Math.sin((Math.PI * progress) / (2 * m))
                        : progress >= (1 - m)
                        ? Math.sin((Math.PI * (progress - (1 - 2 * m)) / (2 * m)))
                        : 1F));

                // TODO: Make position configurable.
                Window window = MinecraftClient.getInstance().getWindow();
                float centerX = window.getScaledWidth() / 2F;
                float y = window.getScaledHeight() / 8F;

                int color = alpha << 24 | 0x00FFFFFF;

                if (alpha > 0) {
                    drawCenteredText(matrices, textRenderer, mainText, centerX, y, color, LandmarkConfig.MAIN_TEXT_SCALE);

                    if (subText != null) {
                        drawCenteredText(matrices, textRenderer, subText, centerX, y + textRenderer.fontHeight * LandmarkConfig.SUB_TEXT_SCALE * 2, color, LandmarkConfig.SUB_TEXT_SCALE);
                    }
                }
            }
        }

    }

    public void tick(LandmarkHolder cache, long time) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;

        if (player != null && LandmarkCache.getInstance(player.world).isInitialized()) {
            Text mainText = null;
            Text subText = null;

            for (LandmarkType type : LandmarkTypeRegistry.getRegistered()) {
                Optional<Pair<Integer, LandmarkType>> testResults = type.test(player.getEntityWorld(), player.getBlockPos(), null);


                if (testResults.isPresent() && !nonFeatureLandmarkStatuses.containsKey(type.getId())) {
                    mainText = type.generateName();
                    nonFeatureLandmarkStatuses.put(type.getId(), true);
                } else if (testResults.isPresent() && nonFeatureLandmarkStatuses.containsKey(type.getId())) {
                    subText = type.generateName();
                } else if (!testResults.isPresent()) {
                    nonFeatureLandmarkStatuses.remove(type.getId());
                }
            }

            Map<UUID, Boolean> visited = new HashMap<>();

            for (Landmark landmark : cache.getLandmarks(new ChunkPos(player.getBlockPos()))) {
                UUID id = landmark.getId();
                boolean bl = (landmark.contains(player.getBlockPos()));
                visited.put(id, visited.getOrDefault(landmark.getId(), false) || bl);

                if (bl && !landmarkStatuses.containsKey(id)) {
                    if (mainText != null) {
                        subText = mainText;
                    }

                    mainText = landmark.getName();
                    landmarkStatuses.put(id, LandmarkConfig.NOTIFICATION_TIME);

                    break;
                }
            }

            for (UUID id : landmarkStatuses.keySet()) {
                Landmark landmark = cache.getLandmark(id);
                if (landmark == null) {
                    landmarkStatuses.remove(id);
                } else if (!landmark.contains(player.getBlockPos())) {
                    landmarkStatuses.computeIfPresent(id, (k, v) -> v - 1);
                }
            }

            landmarkStatuses.entrySet().removeIf(e -> e.getValue() <= 0);

            if (mainText != null && !this.isBusy()) {
                this.display(mainText, subText, time);
            }
        }
    }

    public void display(@NotNull Text mainText, @Nullable Text subText, float startTime) {
        this.mainText = mainText;
        this.subText = subText;
        this.startTime = startTime;
    }

    public boolean isBusy() {
        return this.mainText != null;
    }

    public void clear() {
        this.mainText = null;
        this.subText = null;
        this.startTime = 0;
    }
}
