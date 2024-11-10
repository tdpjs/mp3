package fsft.fsftbuffer;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

public class FSFTBuffer<B extends Bufferable> {
    private final Duration timeout;
    private final int capacity;
    private final ConcurrentHashMap<String, TimestampedEntry> buffer;

    private class TimestampedEntry {
        B value;
        Instant lastAccessed;

        TimestampedEntry(B value) {
            this.value = value;
            this.lastAccessed = Instant.now();
        }

        void refresh() {
            lastAccessed = Instant.now();
        }

        boolean isExpired() {
            return Duration.between(lastAccessed, Instant.now()).compareTo(timeout) > 0;
        }

        @Override
        public String toString() {
            return "Value: " + value +
                    ", Last accessed: " + lastAccessed;
        }
    }

    /* the default buffer size is 32 objects */
    public static final int DEFAULT_CAPACITY = 32;

    /* the default timeout value is 180 seconds */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(180);

    /**
     * Create a buffer with a fixed capacity and a timeout value.
     * Objects in the buffer that have not been refreshed within the
     * timeout period are removed from the cache.
     *
     * @param capacity the number of objects the buffer can hold
     * @param timeout  the duration, in seconds, an object should
     *                 be in the buffer before it times out
     */
    public FSFTBuffer(int capacity, Duration timeout) {
        this.timeout = timeout;
        this.capacity = capacity;
        this.buffer = new ConcurrentHashMap<>();
    }

    /**
     * Create a buffer with default capacity and timeout values.
     */
    public FSFTBuffer() {
        this(DEFAULT_CAPACITY, DEFAULT_TIMEOUT);
    }

    /**
     * Add a value to the buffer.
     * If the buffer is full, the least recently accessed object
     * is removed to make room for the new object.
     *
     * @param b the value to add to the buffer
     * @return true if the value was successfully added, false otherwise
     */
    public boolean put(B b) {
        if (b == null) {
            return false;
        }
        cleanUp();
        TimestampedEntry existingEntry = buffer.get(b.id());

        if (existingEntry != null) {
            existingEntry.refresh();
            return false;
        }
        if (buffer.size() >= capacity) {
            evictLRU();
        }

        buffer.put(b.id(), new TimestampedEntry(b));
        return true;
    }

    /**
     * Retrieve an object from the buffer.
     *
     * @param id the identifier of the object to be retrieved
     * @return the object that matches the identifier from the buffer
     * @throws ExpireException if object has timed-out
     * @throws IllegalArgumentException if id is null or object with
     *                                  given id does not exist in buffer
     */
    public B get(String id) throws ExpireException {
        if (id == null) {
            throw new IllegalArgumentException("Id must not be null");
        }
        TimestampedEntry entry = buffer.get(id);
        if (entry == null) {
            throw new NoSuchElementException("Object with given id does not exist in buffer");
        }
        if (entry.isExpired()) {
            buffer.remove(id); // Remove expired entry
            throw new ExpireException("Object has timed-out");
        }
        entry.refresh();
        return entry.value;
    }

    /**
     * Update the last refresh time for the object with the provided id.
     * This method is used to mark an object as "not stale" so that its
     * timeout is delayed.
     *
     * @param id the identifier of the object to "touch"
     * @return true if successful and false otherwise
     */
    public boolean touch(String id) {
        if (id == null) {
            throw new IllegalArgumentException("Id must not be null");
        }
        TimestampedEntry entry = buffer.get(id);
        if (entry == null) {
            return false;
        }
        entry.refresh();
        return true;
    }

    private void cleanUp() {
        buffer.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    /**
     * Evicts the least recently used entry from the buffer.
     */
    private void evictLRU() {
        System.out.println("This ran");
        String lruId = null;
        Instant oldestAccess = Instant.MAX;  // Ensure comparison with Instant.MAX

        for (Map.Entry<String, TimestampedEntry> entry : buffer.entrySet()) {
            System.out.println("Checking entry: " + entry.getKey() + ", Last accessed: " + entry.getValue().lastAccessed);
            if (entry.getValue().lastAccessed.isBefore(oldestAccess)) {
                oldestAccess = entry.getValue().lastAccessed;
                lruId = entry.getKey();
                System.out.println("Updated LRU: " + lruId);
            }
        }

        if (lruId != null) {
            System.out.println("Evicting: " + lruId);
            buffer.remove(lruId);
        } else {
            System.out.println("Nothing was removed");
        }
    }

}



