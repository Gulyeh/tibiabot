package abstracts;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

public abstract class ThreadLocker {

    private final Semaphore locker;

    public ThreadLocker() {
        locker = new Semaphore(50);
    }

    public CompletableFuture<Void> lockExecuteAsync(Runnable task, ExecutorService executor){
        return CompletableFuture.runAsync(() -> {
            try {
                locker.acquire();
                task.run();
            } catch (Exception ignore) {
            } finally {
                locker.release();
            }
        }, executor);
    }
}
