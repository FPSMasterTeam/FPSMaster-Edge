package top.fpsmaster.event;

import org.junit.jupiter.api.Test;
import top.fpsmaster.event.events.EventTick;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventDispatcherTest {

    @Test
    void registerIsIdempotentForSameListener() {
        AtomicInteger hits = new AtomicInteger();
        CountingListener listener = new CountingListener(hits);

        EventDispatcher.registerListener(listener);
        EventDispatcher.registerListener(listener);
        EventDispatcher.dispatchEvent(new EventTick());
        assertEquals(1, hits.get(), "duplicate register must not double-fire");

        EventDispatcher.unregisterListener(listener);
        EventDispatcher.dispatchEvent(new EventTick());
        assertEquals(1, hits.get(), "unregister must remove the single handler");
    }

    @Test
    void unregisterUsesIdentityNotClass() {
        AtomicInteger hitsA = new AtomicInteger();
        AtomicInteger hitsB = new AtomicInteger();
        CountingListener a = new CountingListener(hitsA);
        CountingListener b = new CountingListener(hitsB);

        EventDispatcher.registerListener(a);
        EventDispatcher.registerListener(b);
        EventDispatcher.unregisterListener(a);
        EventDispatcher.dispatchEvent(new EventTick());

        assertEquals(0, hitsA.get());
        assertEquals(1, hitsB.get(), "sibling instance of the same class must stay registered");

        EventDispatcher.unregisterListener(b);
        EventDispatcher.dispatchEvent(new EventTick());
        assertEquals(1, hitsB.get());
    }

    private static final class CountingListener {
        private final AtomicInteger hits;

        private CountingListener(AtomicInteger hits) {
            this.hits = hits;
        }

        @Subscribe
        public void onTick(EventTick event) {
            hits.incrementAndGet();
        }
    }
}
