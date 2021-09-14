package midautumn.robot;

import java.util.*;
import java.util.function.Function;

public class Robot {
    /// Attributes
    //final attributes
    private final String name;
    private final double size;

    //internal states
    private Position position = new Position();
    private double direction;
    private World world;

    // memory expand
    private List<Memory<?>> memory = new ArrayList<>();

    // sensor expand
    private List<Sensor<?>> sensors = new ArrayList<>();

    // command expand
    private Queue<Command> todo = new ArrayDeque<>();
    private Function<Robot, List<Command>> program = new Function<Robot, List<Command>>() {
        @Override
        public List<Command> apply(Robot robot) {
            List<Command> commands = new ArrayList<>();
            commands.addAll(todo);
            return commands;
        }
    };

    /// Methods
    public Robot(String name, double direction, double size) {
        this.name = name;
        this.direction = direction;
        this.size = Math.min(Math.max(0.5, size), 1);
    }


    /// Pre-programmed Commands
    public boolean go(double distance) {
        //step can be negative if the penguin walks backwards
        double sign = Math.signum(distance);
        distance = Math.abs(distance);
        //penguin walks, each step being 0.2m
        while (distance > 0) {
            position.moveBy(sign * Math.min(distance, 0.2), direction);
            world.resolveCollision(this, position);
            distance -= 0.2;
        }
        return true;
    }

    public boolean turnBy(double deltaDirection) {
        direction += deltaDirection;
        return true;
    }

    public boolean turnTo(double newDirection) {
        direction = newDirection;
        return true;
    }

    public boolean say(String text) {
        world.say(this, text);
        return true;
    }

    public boolean paintWorld(Position pos, char blockType) {
        world.setTerrain(pos, blockType);
        return true;
    }


    /// Getters and Setters
    public String getName() {
        return name;
    }

    public double getSize() {
        return size;
    }

    public Position getPosition() {
        return new Position(position);
    }

    public double getDirection() {
        return direction;
    }

    public World getWorld() {
        return world;
    }

    public void spawnInWorld(World world, char spawnMarker) {
        this.world = world;
        this.position = new Position(world.spawnRobotAt(this, spawnMarker));
    }

    // memory expand, method
    public <T> Memory<T> createMemory(Memory<T> newMemory) {
        // take a new Memory object, and adds it to the memory list
        // and returns the object
        Memory<T> temp = new Memory<>(newMemory.getLabel(), newMemory.getData());
        memory.add(newMemory);
        return temp;
    }

    // memory expand, method
    public String memoryToString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < memory.size(); i++) {
            stringBuilder.append("[<mem" + i + ">]");
        }
        return stringBuilder.toString();
    }

    // sensor expand, method
    public void attachSensor(Sensor<?> sensor) {
        sensors.add(sensor);
        sensor.setOwner(this);
    }

    // sensor expand, method
    private void sense() {
        // fetches the sensor data for all connected sensors
        // and lets its processor process the data
        for (Sensor sensor : sensors) {
            sensor.processor.accept(sensor.getData());
        }
    }

    // command expand, method
    public void setProgram(Function<Robot, List<Command>> newProgram) {
        this.program = newProgram;
    }

    // command expand, method
    private void think() {
        setProgram(program);
        List<Command> commands = program.apply(this);
        todo.addAll(commands);
    }

    // command expand, method
    private void act() {
        while (!todo.isEmpty()) {
            Command command = todo.poll();
            boolean execute = command.execute(this);
            if (Objects.equals(execute, false)) {
                return;
            }
        }
    }

    // command expand, method
    public void work() {
        if (todo.isEmpty()) {
            sense();
            think();
        } else {
            act();
        }
    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "\"%s\" position=%s direction=%.2f°", name, position, Math.toDegrees(direction));
    }
}
