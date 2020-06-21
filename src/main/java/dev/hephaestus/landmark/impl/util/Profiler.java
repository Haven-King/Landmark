package dev.hephaestus.landmark.impl.util;

import dev.hephaestus.landmark.impl.LandmarkMod;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

public class Profiler {
    private static Map<String, Long> TIMES = new TreeMap<>();
    private static Map<String, Long> COUNTS = new TreeMap<>();
    private static Stack<Section> STACK = new Stack<>();

    public static void push(String name) {
        STACK.push(new Section(name));
    }

    public static void pop(boolean print) {
        STACK.pop().pop(print);
    }

    public static void report(Logger logger) {
        logger.info("Times per category:");
        for (Map.Entry<String, Long> entry : TIMES.entrySet()) {
            logger.info("  {}: {}", entry.getKey(), ((double) entry.getValue()) / 1_000_000_000.0);
        }

        logger.info("Average time per category:");
        for (Map.Entry<String, Long> entry : TIMES.entrySet()) {
            logger.info("  {}: {}", entry.getKey(), (((double)entry.getValue()) / ((double)COUNTS.get(entry.getKey()))) / 1_000_000_000.0);
        }
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
            Profiler.COUNTS.compute(this.name, (key, val) -> (val == null ? 0 : val) + 1);

            if (print) {
                LandmarkMod.LOG.info("{}: {}ns", this.name, time);
            }
        }
    }
}
