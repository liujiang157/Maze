package midautumn.robot;

import java.util.function.Consumer;

public abstract class Sensor<T> {

    protected Robot owner;
    protected Consumer<T> processor;

    public void setOwner(Robot owner) {
        this.owner = owner;
    }

    public Sensor<T> setProcessor(Consumer<T> processor) {
        this.processor = processor;
        return this;
    }

    public abstract T getData();
}
