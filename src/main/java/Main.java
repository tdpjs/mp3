import fsft.fsftbuffer.Bufferable;
import fsft.fsftbuffer.ExpireException;
import fsft.fsftbuffer.FSFTBuffer;
import fsft.fsftbuffer.TestBufferable;

import java.time.Duration;

public class Main {

    public static void main(String[] args) throws ExpireException, InterruptedException {
        FSFTBuffer<Bufferable> buffer = new FSFTBuffer<>(2, Duration.ofSeconds(1));

        Bufferable b1 = new TestBufferable("1");
        Bufferable b2 = new TestBufferable("2");
        Bufferable b3 = new TestBufferable("3");

        buffer.put(b1);
        buffer.put(b2);
        Thread.sleep(100);
        buffer.get("1"); // Access "1" to make it the most recently used
        buffer.put(b3); // This should evict "2", because "1" was accessed

//        System.out.println(buffer.get("1"));
    }
}
