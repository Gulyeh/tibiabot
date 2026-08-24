package abstracts;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

@Slf4j
public abstract class ThreadLocker {

    private final Semaphore locker;

    public ThreadLocker() {
        locker = new Semaphore(10);
    }

    public ThreadLocker(int value) {
        if(value > 150 || value < 1)
            throw new RuntimeException("Cannot set Thread Locker value above 150 and below 1");
        locker = new Semaphore(value);
    }

    public CompletableFuture<Void> lockExecuteAsync(Runnable task, ExecutorService executor) {
        return CompletableFuture.runAsync(() -> {
            try {
                locker.acquire();
                task.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
               locker.release();
            }
        }, executor);
    }
}
