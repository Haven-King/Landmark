package dev.hephaestus.landmark.impl.util;

import dev.hephaestus.landmark.impl.LandmarkMod;

import java.util.concurrent.atomic.AtomicInteger;

public class ThreadFactory implements java.util.concurrent.ThreadFactory {
	private static final ThreadGroup GROUP = new ThreadGroup(LandmarkMod.MODID);

	private final AtomicInteger THREAD_NUMBER = new AtomicInteger(1);

	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(GROUP, r,
				LandmarkMod.MODID + THREAD_NUMBER.getAndIncrement(),
				0);

		t.setDaemon(true);

		if (t.getPriority() != Thread.NORM_PRIORITY) {
			t.setPriority(Thread.NORM_PRIORITY);
		}

		return t;
	}
}
