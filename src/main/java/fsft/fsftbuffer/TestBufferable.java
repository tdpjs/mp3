package fsft.fsftbuffer;

public class TestBufferable implements Bufferable {
    private String id;

    public TestBufferable(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String toString() {
        return "ID: " + id;
    }
}
