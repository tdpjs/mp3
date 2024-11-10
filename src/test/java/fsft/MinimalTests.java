package fsft;

import fsft.fsftbuffer.Bufferable;
import fsft.fsftbuffer.ExpireException;
import fsft.fsftbuffer.FSFTBuffer;
import java.time.Duration;
import java.util.NoSuchElementException;

import fsft.fsftbuffer.TestBufferable;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MinimalTests {
    @Test
    public void testBufferOperations() throws InterruptedException, ExpireException {
        FSFTBuffer<Bufferable> buffer = new FSFTBuffer<>(2, Duration.ofSeconds(1));

        Bufferable b1 = new TestBufferable("1");
        Bufferable b2 = new TestBufferable("2");
        Bufferable b3 = new TestBufferable("3");

        // Test put and get
        assertTrue(buffer.put(b1)); // Should be added
        assertTrue(buffer.put(b2)); // Should be added
        assertEquals(b1, buffer.get("1"));
        assertEquals(b2, buffer.get("2"));

        // Test eviction when buffer is full
        buffer.put(b3); // Should evict the least recently used (LRU) object
        assertThrows(NoSuchElementException.class, () -> buffer.get("1"));
        assertEquals("2", buffer.get("2").id());
        assertEquals("3", buffer.get("3").id());

        // Test object timeout
        buffer.put(b1); // Add object
        Thread.sleep(1100); // Sleep for slightly more than the timeout
        assertThrows(ExpireException.class, () -> buffer.get("1"));

        // Test touch functionality
        buffer.put(b1);
        Thread.sleep(500); // Less than the timeout
        assertTrue(buffer.touch("1"));
        Thread.sleep(600); // Sleep more than the original timeout
        assertEquals("1", buffer.get("1").id());

        // Test touching a non-existent object
        assertFalse(buffer.touch("4"));

        // Test getting with null id
        IllegalArgumentException getException = assertThrows(IllegalArgumentException.class, () -> buffer.get(null));
        assertEquals("Id must not be null", getException.getMessage());

        // Test putting null value
        assertFalse(buffer.put(null));

        // Test eviction LRU order
        buffer.put(new TestBufferable("1"));
        buffer.put(new TestBufferable("2"));
        Thread.sleep(100); // Wait a bit
        buffer.get("1"); // Access "1" to make it the most recently used
        buffer.put(b3); // This should evict "2", because "1" was accessed
        assertThrows(NoSuchElementException.class, () -> buffer.get("2"));
        assertEquals("1", buffer.get("1").id());
        assertEquals("3", buffer.get("3").id());

        // Test putting an existing object
        buffer.put(new TestBufferable("1"));
        assertFalse(buffer.put(new TestBufferable("1"))); // Adding the same object should not increase the buffer size
        assertEquals("1", buffer.get("1").id());

        // Test timeout for put
        buffer.put(new TestBufferable("1"));
        Thread.sleep(1100); // Wait for timeout
        assertThrows(ExpireException.class, () -> buffer.get("1"));
    }
}


