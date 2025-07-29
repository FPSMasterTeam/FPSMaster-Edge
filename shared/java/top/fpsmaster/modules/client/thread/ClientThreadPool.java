package top.fpsmaster.modules.client.thread;

import java.util.concurrent.*;

// 此工具只应用于只执行一次，或者执行时间可确定的任务，对于重复性或者不确定任务，应该各自处理其逻辑，包括重复线程任务以及超时时间等，而非直接再此处执行。
// 此工具不保证新的任务可以立即执行，可能会因为其他线程延迟执行。
public class ClientThreadPool {
    private final ExecutorService executorService;

    public ClientThreadPool(int threadCount) {
        executorService = Executors.newFixedThreadPool(threadCount);
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
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
