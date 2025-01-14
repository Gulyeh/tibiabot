package cache.interfaces;

import java.util.concurrent.CountDownLatch;

public interface Cachable {
    void refreshCache(CountDownLatch latch);
}
