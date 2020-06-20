package dev.hephaestus.landmark.impl.util;

import dev.hephaestus.landmark.impl.LandmarkMod;

import java.util.HashMap;
import java.util.Stack;

public class Profiler {
    private static HashMap<String, Long> TIMES = new HashMap<>();
    private static Stack<Section> STACK = new Stack<>();

    public static void push(String name) {
        STACK.push(new Section(name));
    }

    public static void pop(boolean print) {
        STACK.pop().pop(print);
    }

    private static class Section {
        private final String name;
        private final long timeStart;

        private Section(String name) {
            this.name = name;
            this.timeStart = System.nanoTime();
        }

        private void pop(boolean print) {
            long time = System.nanoTime() - this.timeStart;
            Profiler.TIMES.compute(this.name, (key, val) -> (val == null ? 0 : val) + time);

            if (print) {
                LandmarkMod.LOG.info("{}: {}ns", this.name, time);
            }
        }
    }
}
