package top.fpsmaster.modules.client.thread;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientThreadPool {
    private final ExecutorService executorService;

    public ClientThreadPool(int threadCount) {
        int n = Math.max(1, threadCount);
        AtomicInteger idx = new AtomicInteger(1);
        executorService = Executors.newFixedThreadPool(n, r -> {
            Thread t = new Thread(r, "FPSMaster-Async-" + idx.getAndIncrement());
            t.setDaemon(true);
            return t;
        });
    }

    public <T> Future<T> execute(Callable<T> task) {
        return executorService.submit(task);
    }

    public Future<?> runnable(Runnable task) {
        return executorService.submit(task);
    }

    public void close() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}



