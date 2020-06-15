package dev.hephaestus.landmark.impl.landmarks;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;

public interface Nameable {
    Text getName();

    @Environment(EnvType.CLIENT)
    int getColor();

    @Environment(EnvType.CLIENT)
    default int r() {
        return (this.getColor() >> 16) & 0xFF;
    }

    @Environment(EnvType.CLIENT)
    default int g() {
        return (this.getColor() >> 8) & 0xFF;
    }

    @Environment(EnvType.CLIENT)
    default int b() {
        return this.getColor() & 0xFF;
    }
}
