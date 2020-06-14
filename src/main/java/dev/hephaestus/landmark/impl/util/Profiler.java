package dev.hephaestus.landmark.impl.util;

import dev.hephaestus.landmark.impl.LandmarkMod;

import java.util.Stack;

public class Profiler {
    private static Stack<Section> STACK = new Stack<>();

    public static void push(String name) {
        STACK.push(new Section(name));
    }

    public static void pop() {
        STACK.pop().pop();
    }

    private static class Section {
        private final String name;
        private final long timeStart;

        private Section(String name) {
            this.name = name;
            this.timeStart = System.nanoTime();
        }

        private void pop() {
            LandmarkMod.LOG.info("{}: {}ns", this.name, System.nanoTime() - this.timeStart);
        }
    }
}
