package dev.hephaestus.landmark.impl.util;

import net.minecraft.util.math.Box;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeMap;

public class BoxCollection {
    private final int maxVolume;
    private final TreeMap<Integer, Set<Box>> boxes = new TreeMap<>();

    private int volume = 0;
    private int size = 0;

    public BoxCollection(int maxVolume) {
        this.maxVolume = maxVolume;
    }

    private static int volume(Box box) {
        return (int) (box.getXLength() * box.getYLength() * box.getZLength());
    }

    public boolean add(Box box) {
        int volume = volume(box);
        if (this.volume + volume <= this.maxVolume) {


            this.boxes.computeIfAbsent(volume, LinkedHashSet::new).add(box);
            this.volume += volume;
            this.size += 1;
            return true;
        }

        return false;
    }

    public Set<Box> getAll() {
        Set<Box> boxes = new LinkedHashSet<>();
        for (Integer key : this.boxes.descendingKeySet()) {
            boxes.addAll(this.boxes.get(key));
        }

        return boxes;
    }
}
