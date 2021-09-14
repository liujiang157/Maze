package midautumn.robot;

import java.util.Locale;

public class Memory<T> {

    private final String label;
    private T data;

    public Memory(String label, T data) {
        this.label = label;
        this.data = data;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, label + ": " + data);
    }
}
