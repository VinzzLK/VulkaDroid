package net.vulkadroid.render.chunk.build;

import net.vulkadroid.Initializer;
import net.vulkadroid.android.AndroidDeviceDetector;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskDispatcher {

    private static ThreadPoolExecutor executor;
    private static final int DEFAULT_THREADS = 3;
    private static final AtomicInteger pending = new AtomicInteger(0);
    private static final ConcurrentLinkedQueue<int[]> buildQueue = new ConcurrentLinkedQueue<>();
    private static boolean running = false;

    public static void initialize() {
        if (running) return;

        int threads = Integer.getInteger("vulkadroid.chunkThreads",
            AndroidDeviceDetector.isAdreno650() ? 4 : DEFAULT_THREADS);

        executor = new ThreadPoolExecutor(
            threads, threads,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(512),
            r -> {
                Thread t = new Thread(r, "VulkaDroid-ChunkBuilder-" + pending.incrementAndGet());
                t.setDaemon(true);
                t.setPriority(Thread.NORM_PRIORITY - 1); // Slightly below main thread
                return t;
            },
            new ThreadPoolExecutor.DiscardOldestPolicy() // Drop oldest pending builds
        );

        running = true;
        Initializer.LOGGER.info("TaskDispatcher started with {} chunk build threads", threads);
    }

    public static void scheduleBuild(int cx, int cy, int cz) {
        if (!running || executor == null) return;
        executor.execute(() -> {
            try {
                BuildTask task = new BuildTask(cx, cy, cz);
                task.run();
            } catch (Exception e) {
                Initializer.LOGGER.warn("Chunk build failed [{},{},{}]: {}", cx, cy, cz, e.getMessage());
            }
        });
    }

    public static void scheduleTask(Runnable task) {
        if (!running || executor == null) return;
        try {
            executor.execute(task);
        } catch (RejectedExecutionException ignored) {
            // Queue full — skip this build cycle
        }
    }

    public static void shutdown() {
        if (!running) return;
        running = false;
        if (executor != null) {
            executor.shutdownNow();
            try {
                executor.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            executor = null;
        }
        Initializer.LOGGER.info("TaskDispatcher shut down");
    }

    public static int getQueueSize() {
        return executor != null ? executor.getQueue().size() : 0;
    }

    public static boolean isRunning() { return running; }
}
